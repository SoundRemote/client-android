package io.github.soundremote.network

import io.github.soundremote.util.Net.uByte
import io.github.soundremote.util.PacketProtocolType
import java.nio.ByteBuffer

/**
 * Custom data for ACK response on a Connect request.
 */
data class AckConnectData(val protocol: PacketProtocolType) {

    companion object {
        /*
        unsigned 8bit   protocol version
        */
        private const val SIZE = 1

        /**
         * Read ACK packet custom data from the source [ByteBuffer].
         * Increments [source] position by [SIZE] on successful read.
         * @param source [ByteBuffer] to read from
         * @return [AckData] instance or null if there is not enough data remaining in [source].
         */
        fun read(source: ByteBuffer): AckConnectData? {
            if (source.remaining() < SIZE) return null
            val protocol = source.uByte
            return AckConnectData(protocol)
        }
    }
}
