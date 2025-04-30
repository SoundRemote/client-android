package io.github.soundremote.network

import io.github.soundremote.util.Net
import io.github.soundremote.util.Net.uShort
import io.github.soundremote.util.PacketRequestIdType
import java.nio.ByteBuffer

data class AckData(val requestId: PacketRequestIdType, val customData: ByteBuffer) {

    companion object {
        const val CUSTOM_DATA_SIZE = 4

        /*
        unsigned 16bit   request id
        4 byte           custom data
        */
        const val SIZE = 6

        /**
         * Read ACK packet data from the source [ByteBuffer].
         * Increments [source] position by [SIZE] on successful read.
         * @param source [ByteBuffer] to read from
         * @return [AckData] instance or null if there is not enough data remaining in [source].
         */
        fun read(source: ByteBuffer): AckData? {
            if (source.remaining() < SIZE) return null
            val id = source.uShort
            val customData = Net.createPacketBuffer(CUSTOM_DATA_SIZE)
            source.get(customData.array())
            return AckData(id, customData)
        }
    }
}
