package io.github.soundremote.audio.sink

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import androidx.annotation.RequiresApi
import io.github.soundremote.BuildConfig
import io.github.soundremote.util.Audio
import io.github.soundremote.util.Audio.CHANNEL_CONFIG
import io.github.soundremote.util.Audio.SAMPLE_ENCODING
import io.github.soundremote.util.Audio.SAMPLE_RATE
import timber.log.Timber
import java.nio.ByteBuffer

/** Multiplication factor to apply to the minimum buffer size requested. */
private const val PCM_BUFFER_MULTIPLICATION_FACTOR = 4

/** Minimum length for the AudioTrack buffer, in milliseconds. */
const val MIN_PCM_BUFFER_DURATION = 250

/** Maximum length for the AudioTrack buffer, in milliseconds. */
private const val MAX_PCM_BUFFER_DURATION = 750

private const val BYTES_PER_SECOND = SAMPLE_RATE * Audio.CHANNELS * Audio.SAMPLE_SIZE

/** Minimum size for the AudioTrack buffer, in bytes. */
private const val MIN_BUFFER_SIZE = BYTES_PER_SECOND * MIN_PCM_BUFFER_DURATION / 1000

/** Maximum size for the AudioTrack buffer, in bytes. */
private const val MAX_BUFFER_SIZE = BYTES_PER_SECOND * MAX_PCM_BUFFER_DURATION / 1000

private const val AUDIO_QUEUE_LIMIT = 10

class PlaybackSink {
    private val sessionId = AudioManager.AUDIO_SESSION_ID_GENERATE
    private val audioTrack = createTrack()
    private var underruns = 0
    private val audioQueue = ArrayDeque<ByteBuffer>()

    fun start() {
        audioTrack.play()
    }

    fun play(audioData: ByteBuffer) {
        if (audioQueue.size > AUDIO_QUEUE_LIMIT) {
            audioQueue.clear()
            Timber.i("AudioQueue reset")
        }
        if (audioData.hasRemaining()) {
            audioQueue.addLast(audioData)
        }
        var bufferOverflow = false
        while (audioQueue.isNotEmpty() && !bufferOverflow) {
            val data = audioQueue.first()
            val bytesRemaining = data.remaining()
            val writeResult = audioTrack.write(data, bytesRemaining, AudioTrack.WRITE_NON_BLOCKING)
            check(writeResult >= 0) { "AudioTrack write error: $writeResult" }
            val bytesWritten: Int = writeResult
            if (bytesWritten == bytesRemaining) {
                audioQueue.removeFirst()
            } else {
                bufferOverflow = true
//                Timber.i("AudioTrack underwrite: $bytesWritten written out of $bytesRemaining")
            }
        }
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val currentUnderruns = audioTrack.underrunCount
            if (currentUnderruns > underruns) {
                underruns = currentUnderruns
                Timber.i("AudioTrack underruns: $currentUnderruns")
            }
        }
    }

    fun stop() {
        audioTrack.pause()
        audioTrack.flush()
        audioQueue.clear()
    }

    fun release() {
        audioTrack.flush()
        audioTrack.release()
    }

    private fun createTrack(): AudioTrack {
        val minBufferSize =
            AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, SAMPLE_ENCODING)
        val bufferSize = getBufferSizeInBytes(minBufferSize)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val audioFormat = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setEncoding(SAMPLE_ENCODING)
            .setChannelMask(CHANNEL_CONFIG)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createAudioTrackV26(audioAttributes, audioFormat, bufferSize)
        } else {
            createAudioTrackV23(audioAttributes, audioFormat, bufferSize)
        }
    }

    private fun getBufferSizeInBytes(minBufferSizeInBytes: Int): Int {
        val targetBufferSize = minBufferSizeInBytes * PCM_BUFFER_MULTIPLICATION_FACTOR
        return targetBufferSize.coerceIn(MIN_BUFFER_SIZE..MAX_BUFFER_SIZE)
    }

    // Suppress range check on setSessionId because its documentation states that
    // AudioManager.AUDIO_SESSION_ID_GENERATE is an acceptable value
    @SuppressLint("Range")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createAudioTrackV26(
        audioAttributes: AudioAttributes,
        audioFormat: AudioFormat,
        bufferSize: Int,
    ): AudioTrack = AudioTrack.Builder()
        .setAudioAttributes(audioAttributes)
        .setAudioFormat(audioFormat)
        .setBufferSizeInBytes(bufferSize)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .setSessionId(sessionId)
        .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
        .build()

    // Suppress range check on setSessionId because its documentation states that
    // AudioManager.AUDIO_SESSION_ID_GENERATE is an acceptable value
    @SuppressLint("Range")
    private fun createAudioTrackV23(
        audioAttributes: AudioAttributes,
        audioFormat: AudioFormat,
        bufferSize: Int,
    ): AudioTrack = AudioTrack.Builder()
        .setAudioAttributes(audioAttributes)
        .setAudioFormat(audioFormat)
        .setBufferSizeInBytes(bufferSize)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .setSessionId(sessionId)
        .build()
}
