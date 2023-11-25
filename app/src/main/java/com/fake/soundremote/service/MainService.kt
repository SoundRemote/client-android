package com.fake.soundremote.service

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
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.fake.soundremote.R
import com.fake.soundremote.audio.AudioPipe
import com.fake.soundremote.audio.AudioPipe.Companion.PIPE_PLAYING
import com.fake.soundremote.data.Event
import com.fake.soundremote.data.EventActionRepository
import com.fake.soundremote.data.Keystroke
import com.fake.soundremote.data.preferences.PreferencesRepository
import com.fake.soundremote.network.Connection
import com.fake.soundremote.util.ACTION_CLOSE
import com.fake.soundremote.util.ConnectionStatus
import com.fake.soundremote.util.Key
import com.fake.soundremote.util.Net
import com.fake.soundremote.util.SystemMessage
import dagger.hilt.android.AndroidEntryPoint
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
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
internal class MainService : MediaBrowserServiceCompat() {

    @Inject
    lateinit var userPreferencesRepo: PreferencesRepository

    @Inject
    lateinit var eventActionRepository: EventActionRepository

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val binder = LocalBinder()

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var notification: Notification
    private var isForeground = false

    private val _systemMessages: Channel<SystemMessage> = Channel(5, BufferOverflow.DROP_OLDEST)
    val systemMessages: ReceiveChannel<SystemMessage>
        get() = _systemMessages
    private var uncompressedAudio = Channel<ByteArray>(5, BufferOverflow.DROP_OLDEST)
    private var opusAudio = Channel<ByteArray>(5, BufferOverflow.DROP_OLDEST)
    private val connection = Connection(uncompressedAudio, opusAudio, _systemMessages)
    private val audioPipe = AudioPipe(uncompressedAudio, opusAudio)
    val connectionStatus = connection.connectionStatus
    private var _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean>
        get() = _isMuted

    // Flag to detect the initial collected compression value
    private var initialCompressionValue = true

    // Call state
    @Suppress("DEPRECATION")
    private lateinit var phoneStateListener: android.telephony.PhoneStateListener
    private lateinit var callStateListener: TelephonyCallback
    private lateinit var telephonyManager: TelephonyManager
    private val callStateExecutor = Executors.newSingleThreadExecutor()

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
                    Log.i(TAG, "Audio compression changed")
                    connection.sendSetFormat(it)
                }
            }
        }

        mediaSession = createMediaSession()
        sessionToken = mediaSession.sessionToken
        notification = createNotification(mediaSession.sessionToken)
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

    private fun updatePlaybackState() {
        if (connectionStatus.value == ConnectionStatus.CONNECTED &&
            !isMuted.value &&
            audioPipe.state != PIPE_PLAYING
        ) {
            if (requestAudioFocus()) {
                Log.i(TAG, "Starting playback")
                audioPipe.start()
                val noisyFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                registerReceiver(becomingNoisyReceiver, noisyFilter)
                connection.processAudio.set(true)
            } else {
                scope.launch {
                    _systemMessages.send(SystemMessage.MESSAGE_AUDIO_FOCUS_REQUEST_FAILED)
                }
                disconnect()
            }
        } else {
            if (audioPipe.state != PIPE_PLAYING) return
            Log.i(TAG, "Stopping playback")
            connection.processAudio.set(false)
            unregisterReceiver(becomingNoisyReceiver)
            abandonAudioFocus()
            audioPipe.stop()
        }
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

    fun sendKeystroke(keystroke: Keystroke) {
        connection.sendKeystroke(keystroke.keyCode, keystroke.mods)
    }

    fun setMuted(value: Boolean) {
        _isMuted.value = value
        updatePlaybackState()
    }

    // Audio focus
    // https://developer.android.com/guide/topics/media-apps/audio-focus

    private val afChangeListener = OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.i(TAG, "Focus gain")
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.i(TAG, "Focus loss")
                setMuted(true)
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.i(TAG, "Focus loss: transient")
                setMuted(true)
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.i(TAG, "Focus loss: transient, can duck")
                setMuted(true)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private lateinit var focusRequest: AudioFocusRequest

    private fun requestAudioFocus(): Boolean {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val res = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(AudioAttributes.Builder().run {
                    setUsage(AudioAttributes.USAGE_MEDIA)
                    setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    build()
                })
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
        return when (res) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> true
            else -> false
        }
    }

    private fun abandonAudioFocus() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(afChangeListener)
        }
    }

    private val becomingNoisyReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                Log.i(TAG, "Becoming noisy")
                setMuted(true)
            }
        }
    }

    // Media

    //https://developer.android.com/about/versions/13/behavior-changes-13#playback-controls
    private fun createMediaSession(): MediaSessionCompat {
        val playbackState = playbackStateBuilder.build()
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, getString(R.string.notification_title))
            .putString(MediaMetadata.METADATA_KEY_ARTIST, getString(R.string.notification_text))
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
                getString(R.string.notification_action_close),
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
            Log.i(TAG, "MediaSession Play")
            super.onPlay()
            sendKey(Key.MEDIA_PLAY_PAUSE.keyCode)
            // Update playback state to change button in media notification
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
        }

        override fun onStop() {
            Log.i(TAG, "MediaSession Stop")
            super.onStop()
            sendKey(Key.MEDIA_STOP.keyCode)
        }

        override fun onPause() {
            Log.i(TAG, "MediaSession Pause")
            super.onPause()
            sendKey(Key.MEDIA_PLAY_PAUSE.keyCode)
            // Update playback state to change button in media notification
            updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
        }

        override fun onSkipToNext() {
            Log.i(TAG, "MediaSession Next")
            super.onSkipToNext()
            sendKey(Key.MEDIA_NEXT.keyCode)
        }

        override fun onSkipToPrevious() {
            Log.i(TAG, "MediaSession Previous")
            super.onSkipToPrevious()
            sendKey(Key.MEDIA_PREV.keyCode)
        }

        override fun onCustomAction(action: String?, extras: Bundle?) {
            Log.i(TAG, "MediaSession $action")
            when (action) {
                NOTIFICATION_ACTION_CLOSE -> {
                    sendBroadcast(Intent(ACTION_CLOSE))
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

    private fun sendKey(key: Int) {
        connection.sendKeystroke(key, 0)
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
            getString(R.string.notification_action_previous),
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
        )
        val pauseAction = NotificationCompat.Action(
            R.drawable.ic_play_pause,
            getString(R.string.notification_action_play_pause),
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_PLAY_PAUSE
            )
        )
        val nextAction = NotificationCompat.Action(
            R.drawable.ic_skip_next,
            getString(R.string.notification_action_next),
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            )
        )
        val closeAction = NotificationCompat.Action(
            NOTIFICATION_ICON_CLOSE,
            getString(R.string.notification_action_close),
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
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
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
                Log.i(TAG, "Call state: PERMISSION DENIED")
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
                Log.i(TAG, "Call state: IDLE")
                scope.launch {
                    eventActionRepository.getKeystrokeByEventId(Event.CALL_END.id)
                        ?.also { sendKeystroke(it) }
                }
            }

            TelephonyManager.CALL_STATE_RINGING -> {
                Log.i(TAG, "Call state: RINGING")
                scope.launch {
                    eventActionRepository.getKeystrokeByEventId(Event.CALL_BEGIN.id)
                        ?.also { sendKeystroke(it) }
                }
            }

            TelephonyManager.CALL_STATE_OFFHOOK -> Log.i(TAG, "Call state: OFFHOOK")
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

    companion object {
        private val TAG = MainService::class.java.simpleName
        private const val NOTIFICATION_ID = 389
        private const val NOTIFICATION_CHANNEL_ID = "sound_remote_channel_id"
        private const val NOTIFICATION_CHANNEL_NAME = "SoundRemote audio service"
        private const val MEDIA_SESSION_TAG = "SoundRemote Media Session"
        private const val EMPTY_MEDIA_ROOT_ID = "empty_root_id"
        private const val NOTIFICATION_ACTION_CLOSE = "ACTION_CLOSE"
        private val NOTIFICATION_ICON_CLOSE = R.drawable.ic_close
    }
}
