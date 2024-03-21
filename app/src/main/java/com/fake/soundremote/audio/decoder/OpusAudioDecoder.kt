package com.fake.soundremote.audio.decoder

import com.fake.jopus.OPUS_OK
import com.fake.jopus.Opus
import com.fake.soundremote.util.Audio.CHANNELS
import com.fake.soundremote.util.Audio.PACKET_DURATION
import com.fake.soundremote.util.Audio.SAMPLE_RATE
import com.fake.soundremote.util.Audio.SAMPLE_SIZE

/**
 * Creates new OpusAudioDecoder
 * @param sampleRate sample rate in Hz, must be 8/12/16/24/48 KHz
 * @param channels number of channels, must be 1 or 2
 * @param packetDuration packet duration in microseconds, must be a multiple of 2.5ms from 2.5ms to 60ms
 */
class OpusAudioDecoder(
    private val sampleRate: Int = SAMPLE_RATE,
    private val channels: Int = CHANNELS,
    private val packetDuration: Int = PACKET_DURATION,
) {
    private val opus = Opus()

    /** Number of samples per channel (frames) in one packet */
    private val framesPerPacket = (sampleRate.toLong() * packetDuration / 1_000_000).toInt()

    /** Number of bytes per PCM audio packet */
    val bytesPerPacket = framesToBytes(framesPerPacket)

    init {
        check(packetDuration in 2_500..60_000) { "Opus decoder packet duration must be from from 2.5 ms to 60 ms" }
        val initResult = opus.initDecoder(sampleRate, channels)
        if (initResult != OPUS_OK) {
            val errorString = opus.strerror(initResult)
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
            val errorString = opus.strerror(framesDecodedOrError)
            throw DecoderException("Opus decode error: $errorString")
        }
        return framesToBytes(framesDecodedOrError)
    }

    private fun framesToBytes(frames: Int): Int {
        return frames * channels * SAMPLE_SIZE
    }
}
