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
import io.github.soundremote.audio.AudioTrackPlayer
import io.github.soundremote.util.Key
import timber.log.Timber

@AndroidEntryPoint
internal class MediaService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: AudioTrackPlayer
    private val actionClose = "action_close"
    private val sessionCommandClose = SessionCommand(actionClose, Bundle.EMPTY)

    private var mainService: MainService? = null
    private var mainServiceBound: Boolean = false

    override fun onCreate() {
        super.onCreate()
        bindMainService()
        player = AudioTrackPlayer(
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
            notificationTitle = getString(R.string.notification_title_template)
                .format(getString(R.string.app_name)),
        )
        mediaSession = createMediaSession(player)
    }

    @OptIn(UnstableApi::class)
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

    @OptIn(UnstableApi::class)
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        player.setShowNotification(false)
        stopSelf()
    }

    // https://developer.android.com/media/implement/surfaces/mobile#config-action-buttons
    private inner class MyCallback : MediaSession.Callback {

        @OptIn(UnstableApi::class)
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(
                    MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
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
            if (customCommand.customAction == actionClose) {
                Timber.i("MediaSession Close")
                if (mainServiceBound) {
                    mainService?.closeApp()
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    @OptIn(UnstableApi::class)
    private fun createMediaSession(player: Player): MediaSession {
        val buttons = listOf(
            CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                .setCustomIconResId(R.drawable.ic_close)
                .setDisplayName(getString(R.string.close))
                .setSessionCommand(sessionCommandClose)
                .setSlots(CommandButton.SLOT_OVERFLOW)
                .build(),
        )
        val mainActivityIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        return MediaSession.Builder(this, player)
            .setMediaButtonPreferences(buttons)
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
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mainServiceBound = false
        }
    }
}
