package io.github.soundremote.audio.decoder

import ashipo.jopus.OPUS_OK
import ashipo.jopus.Opus
import io.github.soundremote.util.Audio.CHANNELS
import io.github.soundremote.util.Audio.PACKET_DURATION
import io.github.soundremote.util.Audio.SAMPLE_RATE
import io.github.soundremote.util.Audio.SAMPLE_SIZE

/**
 * Creates new OpusAudioDecoder
 * @param sampleRate sample rate in Hz, must be 8/12/16/24/48 KHz
 * @param channels number of channels, must be 1 or 2
 * @param packetDuration packet duration in microseconds, must be a multiple of 2.5ms, maximum 60ms
 */
class OpusAudioDecoder(
    private val sampleRate: Int = SAMPLE_RATE,
    private val channels: Int = CHANNELS,
    private val packetDuration: Int = PACKET_DURATION,
) {
    private val opus = Opus()

    /** Number of samples per channel (frames) in one packet */
    val framesPerPacket = (sampleRate.toLong() * packetDuration / 1_000_000).toInt()

    /** Number of bytes per PCM audio packet */
    val bytesPerPacket = framesToBytes(framesPerPacket)

    // 60ms is the maximum packet duration
    val maxPacketsPerPlc = 60_000 / packetDuration

    init {
        check(packetDuration in 2_500..60_000) {
            "Opus decoder packet duration must be from from 2.5 ms to 60 ms"
        }
        val initResult = opus.initDecoder(sampleRate, channels)
        if (initResult != OPUS_OK) {
            val errorString = opus.getErrorString(initResult)
            throw DecoderException("Opus decoder init error: $errorString")
        }
    }

    fun release() {
        opus.releaseDecoder()
    }

    fun decode(encodedData: ByteArray, decodedData: ByteArray): Int {
        val encodedBytes = encodedData.size
        val framesDecodedOrError =
            opus.decode(encodedData, encodedBytes, decodedData, framesPerPacket, 0)
        if (framesDecodedOrError < 0) {
            val errorString = opus.getErrorString(framesDecodedOrError)
            throw DecoderException("Opus decode error: $errorString")
        }
        return framesToBytes(framesDecodedOrError)
    }

    /**
     * Generates audio to fill for missing packets with Opus packet loss concealment (PLC)
     *
     * @param decodedData generated PCM audio
     * @param decodedFrames number of frames of available space in [decodedData]. Needs to be
     * exactly the duration of audio that is missing. Duration must be a multiple of 2.5 ms.
     *
     * @return number of frames generated
     */
    fun plc(decodedData: ByteArray, decodedFrames: Int): Int {
        val framesDecodedOrError = opus.plc(decodedData, decodedFrames)
        if (framesDecodedOrError < 0) {
            val errorString = opus.getErrorString(framesDecodedOrError)
            throw DecoderException("Opus PLC error: $errorString")
        }
        return framesToBytes(framesDecodedOrError)
    }

    private fun framesToBytes(frames: Int): Int {
        return frames * channels * SAMPLE_SIZE
    }
}
