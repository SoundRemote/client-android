package io.github.soundremote.service

import android.os.Looper
import androidx.media3.common.Player
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.BeforeClass
import org.junit.Test

class StreamPlayerTest {

    @Test
    fun playbackState_onCreate_isReady() {
        val subject = createPlayer(applicationLooper = Looper.myLooper()!!)

        subject.playbackState shouldBe Player.STATE_READY
    }

    @Test
    fun isPlaying_onCreate_isTrue() {
        val subject = createPlayer(applicationLooper = Looper.myLooper()!!)

        subject.isPlaying.shouldBeTrue()
    }

    @Test
    fun isPlaying_playbackStateSetToIdle_isFalse() {
        val subject = createPlayer(applicationLooper = Looper.myLooper()!!)

        subject.playbackState = Player.STATE_IDLE

        subject.playbackState shouldBe Player.STATE_IDLE
        subject.isPlaying.shouldBeFalse()
    }

    @Test
    fun isPlaying_playbackStateSetToReady_isTrue() {
        val subject = createPlayer(applicationLooper = Looper.myLooper()!!)

        subject.playbackState = Player.STATE_READY

        subject.playbackState shouldBe Player.STATE_READY
        subject.isPlaying.shouldBeTrue()
    }

    // Test Listener callbacks onIsPlayingChanged, onPlaybackStateChanged

    @Test
    fun listenerCallbacks_playbackStateSetToIdle_areCalledCorrectly() {
        val testPlayerListener = TestPlayerListener()
        val subject = createPlayer(applicationLooper = Looper.myLooper()!!)
        subject.addListener(testPlayerListener)

        // Set to opposite first to trigger listeners update
        subject.playbackState = Player.STATE_READY
        subject.playbackState = Player.STATE_IDLE

        testPlayerListener.playbackState shouldBe Player.STATE_IDLE
        testPlayerListener.isPlaying shouldBe false
    }

    @Test
    fun listenerCallbacks_playbackStateSetToReady_calledCorrectly() {
        val testPlayerListener = TestPlayerListener()
        val subject = createPlayer(applicationLooper = Looper.myLooper()!!)
        subject.addListener(testPlayerListener)

        // Set to opposite first to trigger listeners update
        subject.playbackState = Player.STATE_IDLE
        subject.playbackState = Player.STATE_READY

        testPlayerListener.playbackState shouldBe Player.STATE_READY
        testPlayerListener.isPlaying shouldBe true
    }

    // Test Listener callbacks onMediaMetadataChanged, onTimelineChanged

    @Test
    fun listenerCallbacks_appStateSetToMuted_calledCorrectly() {
        val testPlayerListener = TestPlayerListener()
        val mutedText = "MUTED_TEXT"
        val subject = createPlayer(mutedText = mutedText, applicationLooper = Looper.myLooper()!!)
        subject.addListener(testPlayerListener)

        // Set to opposite first to trigger listeners update
        subject.updateAppState(muted = false)
        subject.updateAppState(muted = true)

        testPlayerListener.metadata?.artist.toString() shouldContain mutedText
        testPlayerListener.timelineMetadata?.artist.toString() shouldContain mutedText
    }

    @Test
    fun listenerCallbacks_appStateSetToDisconnected_calledCorrectly() {
        val testPlayerListener = TestPlayerListener()
        val disconnectedText = "DISCONNECTED_TEXT"
        val subject = createPlayer(
            disconnectedText = disconnectedText,
            applicationLooper = Looper.myLooper()!!,
        )
        subject.addListener(testPlayerListener)

        // Set to opposite first to trigger listeners update
        subject.updateAppState(connected = true)
        subject.updateAppState(connected = false)

        testPlayerListener.metadata?.artist.toString() shouldContain disconnectedText
        testPlayerListener.timelineMetadata?.artist.toString() shouldContain disconnectedText
    }

    @Test
    fun listenerCallbacks_appStateSetToConnectedAndNotMuted_calledCorrectly() {
        val testPlayerListener = TestPlayerListener()
        val subject = createPlayer(applicationLooper = Looper.myLooper()!!)
        subject.addListener(testPlayerListener)

        // Set to opposite first to trigger listeners update
        subject.updateAppState(connected = false, muted = true)
        subject.updateAppState(connected = true, muted = false)

        testPlayerListener.metadata?.artist.shouldBeNull()
        testPlayerListener.timelineMetadata?.artist.shouldBeNull()
    }

    // Utility

    private fun createPlayer(
        onPlay: () -> Unit = {},
        onPause: () -> Unit = {},
        onStop: () -> Unit = {},
        onPrevious: () -> Unit = {},
        onNext: () -> Unit = {},
        applicationLooper: Looper,
        notificationTitle: String = "",
        disconnectedText: String = "",
        mutedText: String = "",
    ): StreamPlayer {
        return StreamPlayer(
            onPlay = onPlay,
            onPause = onPause,
            onStop = onStop,
            onPrevious = onPrevious,
            onNext = onNext,
            applicationLooper = applicationLooper,
            notificationTitle = notificationTitle,
            disconnectedText = disconnectedText,
            mutedText = mutedText,
        )
    }

    companion object {

        @JvmStatic
        @BeforeClass
        fun beforeAll() {
            Looper.prepare()
        }
    }
}
