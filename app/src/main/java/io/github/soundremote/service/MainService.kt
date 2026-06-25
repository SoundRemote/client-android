package io.github.soundremote.service

import android.Manifest
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.os.Binder
import android.os.Build
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.squareup.seismic.ShakeDetector
import dagger.hilt.android.AndroidEntryPoint
import io.github.soundremote.audio.AudioPipe
import io.github.soundremote.audio.AudioPipe.Companion.PIPE_PLAYING
import io.github.soundremote.data.ActionData
import io.github.soundremote.data.ActionType
import io.github.soundremote.data.AppAction
import io.github.soundremote.data.Event
import io.github.soundremote.data.EventActionRepository
import io.github.soundremote.data.Hotkey
import io.github.soundremote.data.HotkeyRepository
import io.github.soundremote.data.preferences.PreferencesRepository
import io.github.soundremote.network.Connection
import io.github.soundremote.util.ACTION_CLOSE
import io.github.soundremote.util.ConnectionState
import io.github.soundremote.util.Key
import io.github.soundremote.util.Net
import io.github.soundremote.util.SystemMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@AndroidEntryPoint
internal class MainService : Service() {

    @Inject
    lateinit var userPreferencesRepo: PreferencesRepository

    @Inject
    lateinit var eventActionRepository: EventActionRepository

    @Inject
    lateinit var hotkeyRepository: HotkeyRepository

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val binder = LocalBinder()

    private val _systemMessages: Channel<SystemMessage> = Channel(5, BufferOverflow.DROP_OLDEST)
    val systemMessages: ReceiveChannel<SystemMessage>
        get() = _systemMessages

    private val uncompressedAudio = Channel<ByteBuffer>(5, BufferOverflow.SUSPEND)
    private val opusAudio = Channel<ByteBuffer>(5, BufferOverflow.SUSPEND)
    private val packetsLost = AtomicInteger()

    private val connection = Connection(uncompressedAudio, opusAudio, packetsLost, _systemMessages)
    private val audioPipe = AudioPipe(uncompressedAudio, opusAudio, packetsLost)
    val connectionState = connection.state
    private var _mutedState = MutableStateFlow(false)
    val mutedState: StateFlow<Boolean>
        get() = _mutedState

    // Flag to detect the initial collected compression value
    private var initialCompressionValue = true

    // Audio focus
    /**
     * Change this variable together with Requesting/abandoning focus, synchronized by [focusLock]
     */
    private var holdingFocus = false
    private val focusLock = Any()

    @Volatile
    private var ignoreAudioFocus = false

    // Call state
    @Suppress("DEPRECATION")
    private lateinit var phoneStateListener: android.telephony.PhoneStateListener
    private lateinit var callStateListener: TelephonyCallback
    private lateinit var telephonyManager: TelephonyManager
    private val callStateExecutor = Executors.newSingleThreadExecutor()

    // Shake
    private var shakeDetector: ShakeDetector? = null
    private var shakeListener: ShakeDetector.Listener? = null

    init {
        scope.launch {
            connection.state.collect {
                when (it) {
                    ConnectionState.CONNECTED, ConnectionState.DISCONNECTED -> {
                        updatePlaybackState()
                    }

                    else -> {}
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Update audio compression when changed by user
        scope.launch {
            userPreferencesRepo.audioCompressionFlow.collect {
                if (initialCompressionValue) {
                    initialCompressionValue = false
                } else {
                    Timber.i("Audio compression changed")
                    connection.sendSetFormat(it)
                }
            }
        }

        // Shake listener
        scope.launch {
            eventActionRepository.getShakeEventFlow().collect {
                if (it == null) {
                    stopShakeDetection()
                } else {
                    startShakeDetection()
                }
            }
        }
        scope.launch {
            userPreferencesRepo.ignoreAudioFocusFlow.collect { ignore ->
                ignoreAudioFocus = ignore
                if (ignore) {
                    // Abandon audio focus in case we have it
                    abandonAudioFocus()
                } else {
                    // If currently playing audio, should get audio focus or mute if denied
                    if (audioPipe.state == PIPE_PLAYING) {
                        getFocusOrMute()
                    }
                }
            }
        }
        registerCallStateListener()
    }

    override fun onDestroy() {
        stopProcessing()
        audioPipe.release()
        scope.cancel()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        stopProcessing()
        stopSelf()
    }

    private fun stopProcessing() {
        disconnect()
        unregisterCallStateListener()
    }

    // Binding

    inner class LocalBinder : Binder() {
        fun getService(): MainService = this@MainService
    }

    override fun onBind(intent: Intent) = binder

    private fun updatePlaybackState() {
        if (connectionState.value == ConnectionState.CONNECTED &&
            !mutedState.value &&
            audioPipe.state != PIPE_PLAYING
        ) {
            if (ignoreAudioFocus || getFocusOrMute()) {
                startPlayback()
            }
        } else {
            stopPlayback()
        }
    }

    private val becomingNoisyFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)

    private fun startPlayback() {
        if (audioPipe.state == PIPE_PLAYING) return
        Timber.i("Starting playback")
        audioPipe.start()
        registerReceiver(becomingNoisyReceiver, becomingNoisyFilter)
        connection.processAudio = true
    }

    private fun stopPlayback() {
        if (audioPipe.state != PIPE_PLAYING) return
        Timber.i("Stopping playback")
        connection.processAudio = false
        try {
            unregisterReceiver(becomingNoisyReceiver)
        } catch (_: IllegalArgumentException) {
            // if the receiver was not previously registered or already unregistered
        }
        abandonAudioFocus()
        audioPipe.stop()
    }

    // Service API

    fun connect(serverAddress: String) {
        scope.launch {
            val serverPort = userPreferencesRepo.getServerPort()
            val clientPort = userPreferencesRepo.getClientPort()
            @Net.Compression val compression = userPreferencesRepo.getAudioCompression()
            connection.connect(serverAddress, serverPort, clientPort, compression)
        }
    }

    fun disconnect() {
        scope.launch {
            connection.disconnect()
        }
    }

    fun sendHotkey(hotkey: Hotkey) = scope.launch {
        connection.sendHotkey(hotkey.keyCode, hotkey.mods)
    }

    fun sendKey(key: Key) = scope.launch {
        connection.sendHotkey(key.keyCode)
    }

    private suspend fun sendHotkey(hotkeyId: Int) {
        hotkeyRepository.getById(hotkeyId)?.let {
            sendHotkey(it)
        }
    }

    fun setMuted(value: Boolean) {
        _mutedState.value = value
        updatePlaybackState()
    }

    fun closeApp() {
        sendBroadcast(Intent(ACTION_CLOSE).setPackage(packageName))
    }

    // Audio focus
    // https://developer.android.com/media/optimize/audio-focus

    private val afChangeListener = OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                Timber.i("Focus gain")
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                Timber.i("Focus loss")
                setMuted(true)
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Timber.i("Focus loss: transient")
                setMuted(true)
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Timber.i("Focus loss: transient, can duck")
                setMuted(true)
            }
        }
    }

    /**
     * Requests audio focus. If the request is denied, sends the system message and mutes.
     * @return true if audio focus was gained successfully, false otherwise
     */
    private fun getFocusOrMute(): Boolean {
        if (requestAudioFocus()) return true
        setMuted(true)
        scope.launch {
            _systemMessages.send(SystemMessage.MESSAGE_AUDIO_FOCUS_REQUEST_FAILED)
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private lateinit var focusRequest: AudioFocusRequest

    private fun requestAudioFocus(): Boolean = synchronized(focusLock) {
        if (holdingFocus) return true
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val requestResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(afChangeListener)
                .build()
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                afChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        holdingFocus = requestResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        holdingFocus.also { Timber.i("Focus was granted: $it") }
    }


    private fun abandonAudioFocus() {
        synchronized(focusLock) {
            if (!holdingFocus) return
            Timber.i("Abandoning focus")
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioManager.abandonAudioFocusRequest(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(afChangeListener)
            }
            holdingFocus = false
        }
    }

    private val becomingNoisyReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                Timber.i("Becoming noisy")
                setMuted(true)
            }
        }
    }

    // Call state

    private fun registerCallStateListener() {
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.READ_PHONE_STATE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Timber.i("Call state: PERMISSION DENIED")
                return
            }
            callStateListener = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    onCallStateEvent(state)
                }
            }
            telephonyManager.registerTelephonyCallback(callStateExecutor, callStateListener)
        } else {
            registerPhoneStateListener()
        }
    }

    @Suppress("DEPRECATION")
    private fun registerPhoneStateListener() {
        phoneStateListener = object : android.telephony.PhoneStateListener() {
            @Deprecated("Deprecated in Java")
            override fun onCallStateChanged(state: Int, incomingNumber: String) {
                super.onCallStateChanged(state, incomingNumber)
                onCallStateEvent(state)
            }
        }
        val events = android.telephony.PhoneStateListener.LISTEN_CALL_STATE
        telephonyManager.listen(phoneStateListener, events)
    }

    private fun onCallStateEvent(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_IDLE -> {
                Timber.i("Call state: IDLE")
                scope.launch {
                    eventActionRepository.getById(Event.CALL_END.id)
                        ?.let { executeAction(it.action) }
                }
            }

            TelephonyManager.CALL_STATE_RINGING -> {
                Timber.i("Call state: RINGING")
                scope.launch {
                    eventActionRepository.getById(Event.CALL_BEGIN.id)
                        ?.let { executeAction(it.action) }
                }
            }

            TelephonyManager.CALL_STATE_OFFHOOK -> Timber.i("Call state: OFFHOOK")
        }
    }

    private suspend fun executeAction(action: ActionData) {
        when (action.actionType) {
            ActionType.APP.id -> {
                when (action.actionId) {
                    AppAction.CONNECT.id -> {
                        val address = userPreferencesRepo.getServerAddress()
                        connect(address)
                    }

                    AppAction.DISCONNECT.id -> disconnect()
                    AppAction.MUTE.id -> setMuted(true)
                    AppAction.UNMUTE.id -> setMuted(false)
                }
            }

            ActionType.HOTKEY.id -> {
                sendHotkey(action.actionId)
            }
        }
    }

    private fun unregisterCallStateListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (::callStateListener.isInitialized) {
                telephonyManager.unregisterTelephonyCallback(callStateListener)
            }
        } else {
            unregisterPhoneStateListener()
        }
    }

    @Suppress("DEPRECATION")
    private fun unregisterPhoneStateListener() {
        if (::phoneStateListener.isInitialized) {
            telephonyManager.listen(
                phoneStateListener,
                android.telephony.PhoneStateListener.LISTEN_NONE
            )
        }
    }

    // Shake detection

    private fun stopShakeDetection() {
        shakeDetector?.let { sd ->
            sd.stop()
            shakeDetector = null
            shakeListener = null
            Timber.i("Shake detection stopped")
        }
    }

    private fun startShakeDetection() {
        if (shakeDetector != null) return
        shakeListener = getShakeListener()
        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        shakeDetector = ShakeDetector(shakeListener).apply {
            start(sensorManager, SensorManager.SENSOR_DELAY_GAME)
        }
        Timber.i("Shake detection started")
    }

    private fun getShakeListener() = ShakeDetector.Listener {
        Timber.i("Shake detected")
        scope.launch {
            eventActionRepository.getById(Event.SHAKE.id)?.let {
                executeAction(it.action)
            }
        }
    }
}
