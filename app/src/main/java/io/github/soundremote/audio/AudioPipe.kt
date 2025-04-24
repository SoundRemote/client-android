package io.github.soundremote.audio

import androidx.annotation.IntDef
import io.github.soundremote.audio.decoder.OpusAudioDecoder
import io.github.soundremote.audio.sink.PlaybackSink
import io.github.soundremote.util.Audio.PACKET_CONCEAL_LIMIT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger

class AudioPipe(
    private val uncompressedAudio: ReceiveChannel<ByteBuffer>,
    private val opusAudio: ReceiveChannel<ByteBuffer>,
    private val packetsLost: AtomicInteger,
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val decoder = OpusAudioDecoder()
    private val playback = PlaybackSink()

    // An audio packet worth of silence
    private val silencePacket = ByteBuffer.allocate(decoder.bytesPerPacket)

    private var playJob: Job? = null
    private var stopJob: Job? = null
    private val playLock = Any()
    private val stopLock = Any()

    @Volatile
    @PipeState
    var state: Int = PIPE_STOPPED
        private set

    fun start() {
        if (state == PIPE_RELEASED) {
            throw IllegalStateException("Can't start(): AudioPipe is released")
        }
        synchronized(playLock) {
            if (playJob?.isActive == true) return
            playJob = scope.launch {
                stopJob?.join()
                state = PIPE_PLAYING
                playback.start()
                while (isActive) {
                    select {
                        uncompressedAudio.onReceive { audio ->
                            concealLossesUncompressed()

                            playback.play(audio)
                        }
                        opusAudio.onReceive { audio ->
                            concealLossesOpus()

                            val encoded = ByteArray(audio.remaining())
                            audio.get(encoded)
                            val decoded = ByteBuffer.allocate(decoder.bytesPerPacket)
                            val decodedBytes = decoder.decode(encoded, decoded.array())
                            decoded.limit(decodedBytes)
                            playback.play(decoded)
                        }
                    }
                }
            }
        }
    }

    private fun packetsLost(): Int = packetsLost.getAndSet(0)
        .takeIf { it < PACKET_CONCEAL_LIMIT } ?: 0

    private fun concealLossesUncompressed() {
        val packetsToConceal = packetsLost()
        repeat(packetsToConceal) {
            playback.play(silencePacket.duplicate())
        }
    }

    private fun concealLossesOpus() {
        var packetsToConceal = packetsLost()
        while (packetsToConceal > 0) {
            val packets = packetsToConceal.coerceAtMost(decoder.maxPacketsPerPlc)
            val decodedData = ByteBuffer.allocate(decoder.bytesPerPacket * packets)
            val decodedBytes = decoder.plc(decodedData.array(), decoder.framesPerPacket * packets)
            decodedData.limit(decodedBytes)
            playback.play(decodedData)
            packetsToConceal -= packets
        }
    }

    fun stop() {
        if (state == PIPE_RELEASED) {
            throw IllegalStateException("Can't stop(): AudioPipe is released")
        }
        synchronized(stopLock) {
            if (stopJob?.isActive == true) return
            stopJob = scope.launch {
                playJob?.cancelAndJoin()
                state = PIPE_STOPPED
                playback.stop()
            }
        }
    }

    fun release() {
        state = PIPE_RELEASED
        scope.launch {
            playJob?.cancelAndJoin()
            playJob = null
            decoder.release()
            playback.release()
        }
    }

    companion object {
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(PIPE_PLAYING, PIPE_STOPPED, PIPE_RELEASED)
        annotation class PipeState

        internal const val PIPE_PLAYING = 1
        internal const val PIPE_STOPPED = 2
        internal const val PIPE_RELEASED = 3
    }
}
