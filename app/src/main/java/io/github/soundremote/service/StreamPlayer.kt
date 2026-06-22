package io.github.soundremote.service

import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.ui.util.fastJoinToString
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.ListenerSet
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi

@OptIn(UnstableApi::class)
class StreamPlayer(
    private val onPlay: () -> Unit,
    private val onPause: () -> Unit,
    private val onStop: () -> Unit,
    private val onPrevious: () -> Unit,
    private val onNext: () -> Unit,
    private val applicationLooper: Looper,
    private val notificationTitle: String,
    private val disconnectedText: String,
    private val mutedText: String,
) : Player {

    private var appConnected = false
    private var appMuted = false

    private val listeners = ListenerSet<Player.Listener>(applicationLooper)
    private var playerPlaybackState = Player.STATE_READY
    private var mediaData = MediaMetadata.Builder()
        .setTitle(notificationTitle)
        .setArtist(makeAppStateText(appConnected, appMuted))
        .build()
    private val mediaItem = MediaItem.Builder()
        .setMediaMetadata(mediaData)
        .build()
    private val streamTimeline = StreamTimeline(mediaItem)
    private var currentTimeline: Timeline = streamTimeline

    fun setShowNotification(show: Boolean) {
        verifyThread()
        if (show xor (playerPlaybackState == Player.STATE_IDLE)) return
        playerPlaybackState = if (show) Player.STATE_READY else Player.STATE_IDLE
        listeners.queueEvent(Player.EVENT_PLAYBACK_STATE_CHANGED) { listener ->
            listener.onPlaybackStateChanged(playerPlaybackState)
        }
        listeners.queueEvent(Player.EVENT_IS_PLAYING_CHANGED) { listener ->
            listener.onIsPlayingChanged(isPlaying)
        }
        listeners.flushEvents()
    }

    // TODO: also update mediaItem and timeline?
    fun updateAppState(connected: Boolean, muted: Boolean) {
        verifyThread()
        if (connected == appConnected && muted == appMuted) {
            return
        }
        appConnected = connected
        appMuted = muted
        val metadataBuilder = MediaMetadata.Builder().setTitle(notificationTitle)
        if (!connected || muted) {
            metadataBuilder.setArtist(makeAppStateText(connected, muted))
        }
        mediaData = metadataBuilder.build()
        listeners.sendEvent(Player.EVENT_MEDIA_METADATA_CHANGED) { listener ->
            listener.onMediaMetadataChanged(mediaData)
        }
    }

    private fun makeAppStateText(connected: Boolean, muted: Boolean) = buildList {
        if (!connected) add(disconnectedText)
        if (muted) add(mutedText)
    }.fastJoinToString()

    override fun play() = onPlay()
    override fun pause() = onPause()
    override fun stop() = onStop()
    override fun seekToPrevious() = onPrevious()
    override fun seekToNext() = onNext()

    override fun getPlaybackState(): Int {
        verifyThread()
        return playerPlaybackState
    }

    override fun getPlayWhenReady() = true
    override fun isPlaying() = playWhenReady && playbackState == Player.STATE_READY
    override fun setPlayWhenReady(playWhenReady: Boolean) {}

    private val playerAvailableCommands = Player.Commands.Builder().addAll(
        Player.COMMAND_GET_METADATA,
        Player.COMMAND_GET_TIMELINE,
        Player.COMMAND_RELEASE,
        Player.COMMAND_PLAY_PAUSE,
        Player.COMMAND_STOP,
        Player.COMMAND_SEEK_TO_NEXT,
        Player.COMMAND_SEEK_TO_PREVIOUS,
    ).build()

    override fun getAvailableCommands() = playerAvailableCommands
    override fun isCommandAvailable(command: Int) = availableCommands.contains(command)

    override fun release() {
        verifyThread()
        listeners.release()
    }

    // Command independent

    override fun getApplicationLooper() = applicationLooper // Don't verify thread
    override fun canAdvertiseSession() = true
    override fun getPlaybackSuppressionReason() = Player.PLAYBACK_SUPPRESSION_REASON_NONE
    override fun addListener(listener: Player.Listener) = listeners.add(listener)
    override fun removeListener(listener: Player.Listener) {
        verifyThread()
        listeners.remove(listener)
    }

    override fun prepare() {}
    override fun isLoading() = false
    override fun getBufferedPercentage() = 0
    override fun getShuffleModeEnabled() = false
    override fun getRepeatMode() = Player.REPEAT_MODE_OFF
    override fun getSeekBackIncrement() = 0L
    override fun getSeekForwardIncrement() = 0L
    override fun getMaxSeekToPreviousPosition() = 0L
    override fun getPlaybackParameters() = PlaybackParameters.DEFAULT
    override fun getTrackSelectionParameters() = TrackSelectionParameters.DEFAULT
    override fun getPlayerError() = null
    override fun getCurrentManifest() = null
    override fun getDeviceInfo() = DeviceInfo.UNKNOWN
    override fun getVideoSize() = VideoSize.UNKNOWN
    override fun getSurfaceSize() = Size.UNKNOWN

    // COMMAND_GET_METADATA

    override fun getMediaMetadata(): MediaMetadata {
        verifyThread()
        return mediaData
    }

    override fun getPlaylistMetadata() = MediaMetadata.EMPTY

    // COMMAND_GET_TIMELINE

    override fun getCurrentTimeline(): Timeline {
        verifyThread()
        return currentTimeline
    }

    override fun getCurrentPeriodIndex() = 0
    override fun getMediaItemCount() = 1
    override fun getMediaItemAt(index: Int) = mediaItem
    override fun hasPreviousMediaItem() = false
    override fun hasNextMediaItem() = false

    @Deprecated("Deprecated in Java")
    override fun getCurrentWindowIndex() = getCurrentMediaItemIndex()
    override fun getCurrentMediaItemIndex() = 0

    @Deprecated("Deprecated in Java")
    override fun getNextWindowIndex() = getNextMediaItemIndex()
    override fun getNextMediaItemIndex() = C.INDEX_UNSET

    @Deprecated("Deprecated in Java")
    override fun getPreviousWindowIndex() = getPreviousMediaItemIndex()
    override fun getPreviousMediaItemIndex() = C.INDEX_UNSET

    // COMMAND_SEEK_TO_NEXT_MEDIA_ITEM

    override fun seekToNextMediaItem() {}

    // COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM

    override fun seekToPreviousMediaItem() {}

    // COMMAND_SET_REPEAT_MODE

    override fun setRepeatMode(repeatMode: Int) {}

    // COMMAND_SET_SHUFFLE_MODE

    override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {}

    // COMMAND_SEEK_TO_DEFAULT_POSITION

    override fun seekToDefaultPosition() {}

    // COMMAND_SEEK_TO_MEDIA_ITEM

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {}
    override fun seekToDefaultPosition(mediaItemIndex: Int) {}

    // COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM

    override fun seekTo(positionMs: Long) {}

    // COMMAND_SEEK_BACK

    override fun seekBack() {}

    // COMMAND_SEEK_FORWARD

    override fun seekForward() {}

    // COMMAND_SET_SPEED_AND_PITCH

    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) {}
    override fun setPlaybackSpeed(speed: Float) {}

    // COMMAND_GET_TRACKS

    override fun getCurrentTracks() = Tracks.EMPTY

    // COMMAND_SET_TRACK_SELECTION_PARAMETERS

    override fun setTrackSelectionParameters(parameters: TrackSelectionParameters) {}

    // COMMAND_SET_PLAYLIST_METADATA

    override fun setPlaylistMetadata(mediaMetadata: MediaMetadata) {}

    //  COMMAND_CHANGE_MEDIA_ITEMS

    override fun setMediaItems(mediaItems: List<MediaItem>) {}
    override fun setMediaItems(mediaItems: List<MediaItem>, resetPosition: Boolean) {}
    override fun setMediaItems(
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ) {
    }

    override fun addMediaItem(mediaItem: MediaItem) {}
    override fun addMediaItem(index: Int, mediaItem: MediaItem) {}
    override fun addMediaItems(mediaItems: List<MediaItem>) {}
    override fun addMediaItems(index: Int, mediaItems: List<MediaItem>) {}
    override fun moveMediaItem(currentIndex: Int, newIndex: Int) {}
    override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int) {}
    override fun replaceMediaItem(index: Int, mediaItem: MediaItem) {}
    override fun replaceMediaItems(fromIndex: Int, toIndex: Int, mediaItems: List<MediaItem>) {}
    override fun removeMediaItem(index: Int) {}
    override fun removeMediaItems(fromIndex: Int, toIndex: Int) {}
    override fun clearMediaItems() {}

    // COMMAND_SET_MEDIA_ITEM

    override fun setMediaItem(mediaItem: MediaItem) {}
    override fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long) {}
    override fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean) {}

    // COMMAND_GET_CURRENT_MEDIA_ITEM

    override fun getCurrentMediaItem() = mediaItem

    @Deprecated("Deprecated in Java")
    override fun isCurrentWindowDynamic() = isCurrentMediaItemDynamic()
    override fun isCurrentMediaItemDynamic() = false

    @Deprecated("Deprecated in Java")
    override fun isCurrentWindowLive() = isCurrentMediaItemLive()
    override fun isCurrentMediaItemLive() = false

    @Deprecated("Deprecated in Java")
    override fun isCurrentWindowSeekable() = isCurrentMediaItemSeekable()
    override fun isCurrentMediaItemSeekable() = false

    override fun getCurrentLiveOffset() = C.TIME_UNSET
    override fun isPlayingAd() = false
    override fun getCurrentAdGroupIndex() = C.INDEX_UNSET
    override fun getCurrentAdIndexInAdGroup() = C.INDEX_UNSET
    override fun getContentDuration() = getDuration()
    override fun getDuration() = C.TIME_UNSET
    override fun getContentPosition() = getCurrentPosition()
    override fun getCurrentPosition() = 0L
    override fun getContentBufferedPosition() = getBufferedPosition()
    override fun getBufferedPosition() = 0L
    override fun getTotalBufferedDuration() = 0L

    // COMMAND_SET_VOLUME

    override fun setVolume(volume: Float) {}
    override fun mute() {}
    override fun unmute() {}

    // COMMAND_GET_VOLUME

    override fun getVolume() = 0f

    // COMMAND_SET_VIDEO_SURFACE

    override fun clearVideoSurface() {}
    override fun clearVideoSurface(surface: Surface?) {}
    override fun setVideoSurface(surface: Surface?) {}
    override fun setVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {}
    override fun clearVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {}
    override fun setVideoSurfaceView(surfaceView: SurfaceView?) {}
    override fun clearVideoSurfaceView(surfaceView: SurfaceView?) {}
    override fun setVideoTextureView(textureView: TextureView?) {}
    override fun clearVideoTextureView(textureView: TextureView?) {}

    // COMMAND_GET_TEXT

    override fun getCurrentCues() = CueGroup.EMPTY_TIME_ZERO

    // COMMAND_GET_DEVICE_VOLUME

    override fun getDeviceVolume() = 0
    override fun isDeviceMuted() = false

    // COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS

    @Deprecated("Deprecated in Java")
    override fun setDeviceVolume(volume: Int) {
    }

    override fun setDeviceVolume(volume: Int, flags: Int) {}

    // COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS

    @Deprecated("Deprecated in Java")
    override fun increaseDeviceVolume() {
    }

    override fun increaseDeviceVolume(flags: Int) {}

    @Deprecated("Deprecated in Java")
    override fun decreaseDeviceVolume() {
    }

    override fun decreaseDeviceVolume(flags: Int) {}

    @Deprecated("Deprecated in Java")
    override fun setDeviceMuted(p0: Boolean) {
    }

    override fun setDeviceMuted(muted: Boolean, flags: Int) {}

    // COMMAND_GET_AUDIO_ATTRIBUTES

    override fun getAudioAttributes() = AudioAttributes.DEFAULT

    // COMMAND_SET_AUDIO_ATTRIBUTES

    override fun setAudioAttributes(audioAttributes: AudioAttributes, handleAudioFocus: Boolean) {}

    private fun verifyThread() {
        if (Thread.currentThread() != applicationLooper.thread) {
            throw IllegalStateException("Player is accessed on a wrong thread: ${Thread.currentThread().name}")
        }
    }
}