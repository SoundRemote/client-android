package com.fake.soundremote.util

import androidx.annotation.IntDef
import com.fake.soundremote.network.ConnectData
import com.fake.soundremote.network.DisconnectData
import com.fake.soundremote.network.KeepAliveData
import com.fake.soundremote.network.KeystrokeData
import com.fake.soundremote.network.PacketData
import com.fake.soundremote.network.PacketHeader
import com.fake.soundremote.network.SetFormatData
import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class ConnectionStatus {
    DISCONNECTED, CONNECTING, CONNECTED
}

typealias PacketSignatureType = UShort
typealias PacketCategoryType = UByte
typealias PacketSizeType = UShort
typealias PacketRequestIdType = UShort
typealias PacketProtocolType = UByte
typealias PacketKeyType = UByte
typealias PacketModsType = UByte

object Net {
    const val PROTOCOL_VERSION: PacketProtocolType = 1u
    const val PROTOCOL_SIGNATURE: PacketSignatureType = 0xA571u

    /**
     *  Audio is send in 10 milliseconds intervals of 48khz, 2 byte per sample, 2 channels signal,
     *  which is 1920 bytes if uncompressed.
     */
    const val RECEIVE_BUFFER_CAPACITY = 2048
    const val SERVER_TIMEOUT_SECONDS = 5

    enum class PacketCategory(val value: PacketCategoryType) {
        CONNECT(0x01u),
        DISCONNECT(0x02u),
        SET_FORMAT(0x03u),
        KEYSTROKE(0x10u),
        AUDIO_DATA_UNCOMPRESSED(0x20u),
        AUDIO_DATA_OPUS(0x21u),
        CLIENT_KEEP_ALIVE(0x30u),
        SERVER_KEEP_ALIVE(0x31u),
        ACK(0xF0u),
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        COMPRESSION_NONE,
        COMPRESSION_64,
        COMPRESSION_128,
        COMPRESSION_192,
        COMPRESSION_256,
        COMPRESSION_320
    )
    annotation class Compression

    const val COMPRESSION_NONE = 0
    const val COMPRESSION_64 = 1
    const val COMPRESSION_128 = 2
    const val COMPRESSION_192 = 3
    const val COMPRESSION_256 = 4
    const val COMPRESSION_320 = 5

    private val keepAlivePacket: ByteBuffer
    private val disconnectPacket: ByteBuffer
    private val BYTE_ORDER: ByteOrder = ByteOrder.BIG_ENDIAN

    init {
        keepAlivePacket =
            createPacket(PacketCategory.CLIENT_KEEP_ALIVE, KeepAliveData(), KeepAliveData.SIZE)
        disconnectPacket =
            createPacket(PacketCategory.DISCONNECT, DisconnectData(), DisconnectData.SIZE)
    }

    fun ByteBuffer.getUByte(): UByte {
        return get().toUByte()
    }

    fun ByteBuffer.getUShort(): UShort {
        return short.toUShort()
    }

    fun ByteBuffer.getUInt(): UInt {
        return int.toUInt()
    }

    fun ByteBuffer.putUByte(value: UByte): ByteBuffer {
        return put(value.toByte())
    }

    fun ByteBuffer.putUShort(value: UShort): ByteBuffer {
        return putShort(value.toShort())
    }

    fun createPacketBuffer(size: Int): ByteBuffer {
        return ByteBuffer.allocate(size).order(BYTE_ORDER)
    }

    private fun createPacket(
        category: PacketCategory,
        data: PacketData,
        dataSize: Int
    ): ByteBuffer {
        val packetSize = PacketHeader.SIZE + dataSize
        val packet = createPacketBuffer(packetSize)
        val header = PacketHeader(category, packetSize)
        header.write(packet)
        data.write(packet)
        packet.rewind()
        return packet
    }

    fun getConnectPacket(
        @Compression compression: Int,
        requestId: PacketRequestIdType
    ): ByteBuffer {
        val data = ConnectData(compression, requestId)
        return createPacket(PacketCategory.CONNECT, data, ConnectData.SIZE)
    }

    fun getDisconnectPacket(): ByteBuffer {
        disconnectPacket.rewind()
        return disconnectPacket
    }

    fun getSetFormatPacket(
        @Compression compression: Int,
        requestId: PacketRequestIdType
    ): ByteBuffer {
        val data = SetFormatData(compression, requestId)
        return createPacket(PacketCategory.SET_FORMAT, data, SetFormatData.SIZE)
    }

    fun getKeystrokePacket(keyCode: PacketKeyType, mods: PacketModsType): ByteBuffer {
        val data = KeystrokeData(keyCode, mods)
        return createPacket(PacketCategory.KEYSTROKE, data, KeystrokeData.SIZE)
    }

    fun getKeepAlivePacket(): ByteBuffer {
        keepAlivePacket.rewind()
        return keepAlivePacket
    }
}
