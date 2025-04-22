package io.github.soundremote.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.MediaMetadata
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.squareup.seismic.ShakeDetector
import dagger.hilt.android.AndroidEntryPoint
import io.github.soundremote.R
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
import io.github.soundremote.util.ConnectionStatus
import io.github.soundremote.util.Key
import io.github.soundremote.util.KeyCode
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
internal class MainService : MediaBrowserServiceCompat() {

    @Inject
    lateinit var userPreferencesRepo: PreferencesRepository

    @Inject
    lateinit var eventActionRepository: EventActionRepository

    @Inject
    lateinit var hotkeyRepository: HotkeyRepository

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val binder = LocalBinder()

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var notification: Notification
    private var isForeground = false

    private val _systemMessages: Channel<SystemMessage> = Channel(5, BufferOverflow.DROP_OLDEST)
    val systemMessages: ReceiveChannel<SystemMessage>
        get() = _systemMessages

    private val uncompressedAudio = Channel<ByteBuffer>(5, BufferOverflow.SUSPEND)
    private val opusAudio = Channel<ByteBuffer>(5, BufferOverflow.SUSPEND)
    private val packetsLost = AtomicInteger()

    private val connection = Connection(uncompressedAudio, opusAudio, packetsLost, _systemMessages)
    private val audioPipe = AudioPipe(uncompressedAudio, opusAudio, packetsLost)
    val connectionStatus = connection.connectionStatus
    private var _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean>
        get() = _isMuted

    // Flag to detect the initial collected compression value
    private var initialCompressionValue = true

    // Audio focus
    /**
     * Change this variable together with Requesting/abandoning focus, synchronized by [focusLock]
     */
    private var holdingFocus = false
    private val focusLock = Any()

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
            connection.connectionStatus.collect {
                when (it) {
                    ConnectionStatus.CONNECTED, ConnectionStatus.DISCONNECTED -> {
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

        mediaSession = createMediaSession()
        sessionToken = mediaSession.sessionToken
        notification = createNotification(mediaSession.sessionToken)

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
    }

    override fun onDestroy() {
        super.onDestroy()

        stopProcessing()
        mediaSession.release()
        audioPipe.release()
        scope.cancel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        if (!isForeground) {
            showForeground()
            startProcessing()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)

        stopProcessing()
        removeForeground()
        stopSelf()
    }

    private fun startProcessing() {
        mediaSession.isActive = true
        registerCallStateListener()
    }

    private fun stopProcessing() {
        disconnect()
        unregisterCallStateListener()
        mediaSession.isActive = false
    }

    private fun showForeground() {
        isForeground = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun removeForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        isForeground = false
    }

    inner class LocalBinder : Binder() {
        fun getService(): MainService = this@MainService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private fun updatePlaybackState() = scope.launch {
        if (connectionStatus.value == ConnectionStatus.CONNECTED &&
            !isMuted.value &&
            audioPipe.state != PIPE_PLAYING
        ) {
            val canStart = userPreferencesRepo.getIgnoreAudioFocus() || getFocusOrMute()
            if (canStart) {
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
        unregisterReceiver(becomingNoisyReceiver)
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

    fun sendHotkey(hotkey: Hotkey) {
        connection.sendHotkey(hotkey.keyCode, hotkey.mods)
    }

    fun sendKey(key: Key) {
        connection.sendHotkey(key.keyCode)
    }

    private suspend fun sendHotkey(hotkeyId: Int) {
        hotkeyRepository.getById(hotkeyId)?.let {
            sendHotkey(it)
        }
    }

    fun setMuted(value: Boolean) {
        _isMuted.value = value
        updatePlaybackState()
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
    private suspend fun getFocusOrMute(): Boolean {
        if (requestAudioFocus()) return true
        _systemMessages.send(SystemMessage.MESSAGE_AUDIO_FOCUS_REQUEST_FAILED)
        setMuted(true)
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

    // Media

    //https://developer.android.com/about/versions/13/behavior-changes-13#playback-controls
    private fun createMediaSession(): MediaSessionCompat {
        val playbackState = playbackStateBuilder.build()
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, notificationTitle)
            .build()
        return MediaSessionCompat(this, MEDIA_SESSION_TAG).apply {
            setCallback(MediaCallback())
            setPlaybackState(playbackState)
            setMetadata(metadata)
        }
    }

    private val playbackStateBuilder by lazy {
        PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY_PAUSE
                        or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            )
            .addCustomAction(
                NOTIFICATION_ACTION_CLOSE,
                closeActionTitle,
                NOTIFICATION_ICON_CLOSE
            )
            .setState(
                PlaybackStateCompat.STATE_PAUSED,
                PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                1f
            )
    }

    private fun updatePlaybackState(state: Int) {
        mediaSession.setPlaybackState(
            playbackStateBuilder
                .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f)
                .build()
        )
    }

    private inner class MediaCallback : MediaSessionCompat.Callback() {
        override fun onPlay() {
            Timber.i("MediaSession Play")
            super.onPlay()
            sendKey(Key.MEDIA_PLAY_PAUSE.keyCode)
            // Update playback state to change button in media notification
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
        }

        override fun onStop() {
            Timber.i("MediaSession Stop")
            super.onStop()
            sendKey(Key.MEDIA_STOP.keyCode)
        }

        override fun onPause() {
            Timber.i("MediaSession Pause")
            super.onPause()
            sendKey(Key.MEDIA_PLAY_PAUSE.keyCode)
            // Update playback state to change button in media notification
            updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
        }

        override fun onSkipToNext() {
            Timber.i("MediaSession Next")
            super.onSkipToNext()
            sendKey(Key.MEDIA_NEXT.keyCode)
        }

        override fun onSkipToPrevious() {
            Timber.i("MediaSession Previous")
            super.onSkipToPrevious()
            sendKey(Key.MEDIA_PREV.keyCode)
        }

        override fun onCustomAction(action: String?, extras: Bundle?) {
            Timber.i("MediaSession $action")
            when (action) {
                NOTIFICATION_ACTION_CLOSE -> {
                    Intent(ACTION_CLOSE).also { intent ->
                        intent.setPackage(packageName)
                        sendBroadcast(intent)
                    }
                }
            }
            super.onCustomAction(action, extras)
        }

//        @Override
//        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
//            KeyEvent keyEvent = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
//            PlaybackStateCompat state = mMediaSession.getController().getPlaybackState();
//            return super.onMediaButtonEvent(mediaButtonEvent);
//        }
    }

    private fun sendKey(key: KeyCode) {
        connection.sendHotkey(key)
    }

    private fun createNotification(sessionToken: MediaSessionCompat.Token): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        val mediaStyle: NotificationCompat.Style =
            androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(sessionToken)
                .setShowActionsInCompactView(0, 1, 2)

        val previousAction = NotificationCompat.Action(
            R.drawable.ic_skip_previous,
            getString(R.string.key_media_prev),
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
        )
        val pauseAction = NotificationCompat.Action(
            R.drawable.ic_play_pause,
            getString(R.string.key_media_play_pause),
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_PLAY_PAUSE
            )
        )
        val nextAction = NotificationCompat.Action(
            R.drawable.ic_skip_next,
            getString(R.string.key_media_next),
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            )
        )
        val closeAction = NotificationCompat.Action(
            NOTIFICATION_ICON_CLOSE,
            closeActionTitle,
            PendingIntent.getBroadcast(
                this, 0, Intent(ACTION_CLOSE), PendingIntent.FLAG_IMMUTABLE
            )
        )
        val sessionActivityPendingIntent =
            packageManager?.getLaunchIntentForPackage(packageName)?.let { sessionIntent ->
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                PendingIntent.getActivity(this, 0, sessionIntent, flags)
            }
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notificationTitle)
            .setContentIntent(sessionActivityPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .setStyle(mediaStyle)
            .addAction(previousAction)
            .addAction(pauseAction)
            .addAction(nextAction)
            .addAction(closeAction)
            .build()
    }

    private val notificationTitle: String
        get() = getString(R.string.notification_title_template).format(getString(R.string.app_name))

    private val closeActionTitle: String
        get() = getString(R.string.close)

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        // Clients can connect, but this BrowserRoot is an empty hierarchy
        // so onLoadChildren returns nothing. This disables the ability to browse for content.
        return BrowserRoot(EMPTY_MEDIA_ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaBrowserCompat.MediaItem>>
    ) {
        // Browsing is not allowed
        result.sendResult(null)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val notificationChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)
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

    companion object {
        private const val NOTIFICATION_ID = 389
        private const val NOTIFICATION_CHANNEL_ID = "sound_remote_channel_id"
        private const val NOTIFICATION_CHANNEL_NAME = "SoundRemote audio service"
        private const val MEDIA_SESSION_TAG = "SoundRemote Media Session"
        private const val EMPTY_MEDIA_ROOT_ID = "empty_root_id"
        private const val NOTIFICATION_ACTION_CLOSE = "ACTION_CLOSE"
        private val NOTIFICATION_ICON_CLOSE = R.drawable.ic_close
    }
}
