package io.github.soundremote.service

import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline

class TestPlayerListener : Player.Listener {

    var playbackState: Int? = null
    var isPlaying: Boolean? = null
    var metadata: MediaMetadata? = null
    var timelineMetadata: MediaMetadata? = null

    override fun onPlaybackStateChanged(playbackState: Int) {
        this.playbackState = playbackState
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        this.isPlaying = isPlaying
    }

    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        metadata = mediaMetadata
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        val window = timeline.getWindow(0, Timeline.Window())
        timelineMetadata = window.mediaItem.mediaMetadata
    }
}
