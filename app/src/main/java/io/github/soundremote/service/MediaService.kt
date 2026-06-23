package io.github.soundremote.service

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import io.github.soundremote.MainActivity
import io.github.soundremote.R
import io.github.soundremote.util.ConnectionStatus
import io.github.soundremote.util.Key
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(UnstableApi::class)
@AndroidEntryPoint
internal class MediaService(dispatcher: CoroutineDispatcher = Dispatchers.Main) :
    MediaSessionService() {

    private val scope = CoroutineScope(dispatcher)
    private var connectionStateCollect: Job? = null
    private var mutedStateCollect: Job? = null

    private var mediaSession: MediaSession? = null
    private lateinit var player: StreamPlayer
    private val actionClose = "action_close"
    private val sessionCommandClose = SessionCommand(actionClose, Bundle.EMPTY)
    private val actionMute = "action_mute"
    private val sessionCommandMute = SessionCommand(actionMute, Bundle.EMPTY)
    private val actionUnmute = "action_unmute"
    private val sessionCommandUnmute = SessionCommand(actionUnmute, Bundle.EMPTY)

    private var mainService: MainService? = null
    private var mainServiceBound: Boolean = false

    private lateinit var muteButton: CommandButton
    private lateinit var unmuteButton: CommandButton
    private lateinit var closeButton: CommandButton

    override fun onCreate() {
        super.onCreate()
        initCommandButtons()
        bindMainService()
        setShowNotificationForIdlePlayer(SHOW_NOTIFICATION_FOR_IDLE_PLAYER_NEVER)
        player = StreamPlayer(
            onPlay = {
                Timber.i("MediaSession Play")
                if (mainServiceBound) {
                    mainService?.sendKey(Key.MEDIA_PLAY_PAUSE)
                }
            },
            onPause = {
                Timber.i("MediaSession Pause")
                if (mainServiceBound) {
                    mainService?.sendKey(Key.MEDIA_PLAY_PAUSE)
                }
            },
            onStop = {
                Timber.i("MediaSession Stop")
                if (mainServiceBound) {
                    mainService?.sendKey(Key.MEDIA_STOP)
                }
            },
            onPrevious = {
                Timber.i("MediaSession Previous")
                if (mainServiceBound) {
                    mainService?.sendKey(Key.MEDIA_PREV)
                }
            },
            onNext = {
                Timber.i("MediaSession Next")
                if (mainServiceBound) {
                    mainService?.sendKey(Key.MEDIA_NEXT)
                }
            },
            applicationLooper = mainLooper,
            notificationTitle = getString(R.string.app_name),
            disconnectedText = getString(R.string.notification_disconnected_text),
            mutedText = getString(R.string.notification_muted_text),
        )
        mediaSession = createMediaSession(player)
    }

    private fun initCommandButtons() {
        unmuteButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setCustomIconResId(R.drawable.ic_volume_mute)
            .setDisplayName(getString(R.string.action_unmute_app))
            .setSessionCommand(sessionCommandUnmute)
            .setSlots(CommandButton.SLOT_OVERFLOW)
            .build()
        muteButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setCustomIconResId(R.drawable.ic_volume_up)
            .setDisplayName(getString(R.string.action_mute_app))
            .setSessionCommand(sessionCommandMute)
            .setSlots(CommandButton.SLOT_OVERFLOW)
            .build()
        closeButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setCustomIconResId(R.drawable.ic_close)
            .setDisplayName(getString(R.string.close))
            .setSessionCommand(sessionCommandClose)
            .setSlots(CommandButton.SLOT_OVERFLOW)
            .build()
    }

    override fun onDestroy() {
        mainServiceBound = false
        unbindService(serviceConnection)
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        player.setShowNotification(false)
        stopSelf()
    }

    // https://developer.android.com/media/implement/surfaces/mobile#config-action-buttons
    private inner class MyCallback : MediaSession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(
                    MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                        .add(sessionCommandMute)
                        .add(sessionCommandUnmute)
                        .add(sessionCommandClose)
                        .build()
                )
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                actionMute -> {
                    Timber.i("MediaSession Mute")
                    if (mainServiceBound) {
                        mainService?.setMuted(true)
                    }
                }

                actionUnmute -> {
                    Timber.i("MediaSession Unmute")
                    if (mainServiceBound) {
                        mainService?.setMuted(false)
                    }
                }

                actionClose -> {
                    Timber.i("MediaSession Close")
                    if (mainServiceBound) {
                        mainService?.closeApp()
                    }
                }

                else -> return super.onCustomCommand(session, controller, customCommand, args)
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    private fun createMediaSession(player: Player): MediaSession {
        val mainActivityIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        return MediaSession.Builder(this, player)
            .setCallback(MyCallback())
            .setSessionActivity(mainActivityIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    // MainService bond

    private fun bindMainService() {
        val intent = Intent(this, MainService::class.java)
        bindService(
            intent,
            serviceConnection,
            0,
        )
    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            val localBinder = binder as MainService.LocalBinder
            mainService = localBinder.getService()
            mainServiceBound = true
            startCollect()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            stopCollect()
            mainServiceBound = false
        }
    }

    private fun startCollect() {
        mainService?.let { service ->
            connectionStateCollect = scope.launch {
                service.connectionStatus.collect {
                    player.updateAppState(connected = it == ConnectionStatus.CONNECTED)
                }
            }
            mutedStateCollect = scope.launch {
                service.isMuted.collect {
                    mediaSession?.setMediaButtonPreferences(makeCommandButtons(it))
                    player.updateAppState(muted = it)
                }
            }
        }
    }

    private fun makeCommandButtons(muted: Boolean) = listOf(
        if (muted) unmuteButton else muteButton,
        closeButton,
    )

    private fun stopCollect() {
        connectionStateCollect?.cancel()
        connectionStateCollect = null
        mutedStateCollect?.cancel()
        mutedStateCollect = null
    }
}
