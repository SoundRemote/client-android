package com.fake.soundremote.util

import com.fake.soundremote.network.ConnectData
import com.fake.soundremote.network.DisconnectData
import com.fake.soundremote.network.KeepAliveData
import com.fake.soundremote.network.HotkeyData
import com.fake.soundremote.network.PacketHeader
import com.fake.soundremote.network.SetFormatData
import com.fake.soundremote.util.Net.COMPRESSION_256
import com.fake.soundremote.util.Net.COMPRESSION_320
import com.fake.soundremote.util.Net.PROTOCOL_VERSION
import com.fake.soundremote.util.Net.calculateGap
import com.fake.soundremote.util.Net.getConnectPacket
import com.fake.soundremote.util.Net.getDisconnectPacket
import com.fake.soundremote.util.Net.getKeepAlivePacket
import com.fake.soundremote.util.Net.getHotkeyPacket
import com.fake.soundremote.util.Net.getSetFormatPacket
import com.fake.soundremote.util.Net.uByte
import com.fake.soundremote.util.Net.uInt
import com.fake.soundremote.util.Net.uShort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.nio.ByteBuffer

@DisplayName("Network utils")
internal class NetworkTest {

    @Nested
    @DisplayName("ByteBuffer.uByte")
    inner class GetUByte {
        @Test
        @DisplayName("reads correctly")
        fun readsCorrectly() {
            val expected: UByte = 0xFAu
            val buf = ByteBuffer.allocate(8)
            buf.put(expected.toByte())
            buf.rewind()

            val actual = buf.uByte

            assertEquals(expected, actual)
        }

        @Test
        @DisplayName("increments the ByteBuffer position by 1")
        fun incrementsPositionCorrectly() {
            val buf = ByteBuffer.allocate(8)

            buf.uByte

            assertEquals(1, buf.position())
        }
    }

    @Nested
    @DisplayName("ByteBuffer.uShort")
    inner class GetUShort {

        @Test
        @DisplayName("reads correctly")
        fun readsCorrectly() {
            val expected: UShort = 0xFA_01u
            val buf = ByteBuffer.allocate(8)
            buf.putShort(expected.toShort())
            buf.rewind()

            val actual = buf.uShort

            assertEquals(expected, actual)
        }

        @Test
        @DisplayName("increments the ByteBuffer position by 2")
        fun incrementsPositionCorrectly() {
            val buf = ByteBuffer.allocate(8)

            buf.uShort

            assertEquals(2, buf.position())
        }
    }

    @Nested
    @DisplayName("ByteBuffer.uInt")
    inner class GetUInt {

        @Test
        @DisplayName("reads correctly")
        fun readsCorrectly() {
            val expected = 0xFA_01_02_03u
            val buf = ByteBuffer.allocate(8)
            buf.putInt(expected.toInt())
            buf.rewind()

            val actual = buf.uInt

            assertEquals(expected, actual)
        }

        @Test
        @DisplayName("increments the ByteBuffer position by 4")
        fun incrementsPositionCorrectly() {
            val buf = ByteBuffer.allocate(8)

            buf.uInt

            assertEquals(4, buf.position())
        }
    }

    @Test
    @DisplayName("getConnectPacket() returns correct packet")
    fun getConnectPacket_returnsCorrectPacket() {
        @Net.Compression val compression = COMPRESSION_320
        val requestId: PacketRequestIdType = 0xf123u
        val expectedSize = PacketHeader.SIZE + ConnectData.SIZE

        val actual = getConnectPacket(compression, requestId)

        assertEquals(actual.remaining(), expectedSize)
        assertEquals(actual.uShort, Net.PROTOCOL_SIGNATURE)
        assertEquals(actual.uByte, Net.PacketCategory.CONNECT.value)
        assertEquals(actual.uShort, expectedSize.toUShort())
        assertEquals(actual.uByte, PROTOCOL_VERSION)
        assertEquals(actual.uShort, requestId)
        assertEquals(actual.uByte, compression.toUByte())
    }

    @Test
    @DisplayName("getSetFormatPacket() returns correct packet")
    fun getSetFormatPacket_returnsCorrectPacket() {
        @Net.Compression val compression = COMPRESSION_256
        val requestId: PacketRequestIdType = 0xfacbu
        val expectedSize = PacketHeader.SIZE + SetFormatData.SIZE

        val actual = getSetFormatPacket(compression, requestId)

        assertEquals(actual.remaining(), expectedSize)
        assertEquals(actual.uShort, Net.PROTOCOL_SIGNATURE)
        assertEquals(actual.uByte, Net.PacketCategory.SET_FORMAT.value)
        assertEquals(actual.uShort, expectedSize.toUShort())
        assertEquals(actual.uShort, requestId)
        assertEquals(actual.uByte, compression.toUByte())
    }

    @Test
    @DisplayName("getHotkeyPacket() returns correct packet")
    fun getHotkeyPacket_returnsCorrectPacket() {
        val keyCode: PacketKeyType = 0xfbu
        val mods: PacketModsType = 0xfau
        val expectedSize = PacketHeader.SIZE + HotkeyData.SIZE

        val actual = getHotkeyPacket(keyCode, mods)

        assertEquals(actual.remaining(), expectedSize)
        assertEquals(actual.uShort, Net.PROTOCOL_SIGNATURE)
        assertEquals(actual.uByte, Net.PacketCategory.HOTKEY.value)
        assertEquals(actual.uShort, expectedSize.toUShort())
        assertEquals(actual.uByte, keyCode)
        assertEquals(actual.uByte, mods)
    }

    @Test
    @DisplayName("getKeepAlivePacket() returns correct packet")
    fun getKeepAlivePacket_returnsCorrectPacket() {
        val expectedSize = PacketHeader.SIZE + KeepAliveData.SIZE

        val actual = getKeepAlivePacket()

        assertEquals(actual.remaining(), expectedSize)
        assertEquals(actual.uShort, Net.PROTOCOL_SIGNATURE)
        assertEquals(actual.uByte, Net.PacketCategory.CLIENT_KEEP_ALIVE.value)
        assertEquals(actual.uShort, expectedSize.toUShort())
    }

    @Test
    @DisplayName("getDisconnectPacket() returns correct packet")
    fun getDisconnectPacket_returnsCorrectPacket() {
        val expectedSize = PacketHeader.SIZE + DisconnectData.SIZE

        val actual = getDisconnectPacket()

        assertEquals(actual.remaining(), expectedSize)
        assertEquals(actual.uShort, Net.PROTOCOL_SIGNATURE)
        assertEquals(actual.uByte, Net.PacketCategory.DISCONNECT.value)
        assertEquals(actual.uShort, expectedSize.toUShort())
    }

    @ParameterizedTest(name = "gap from {0} to {1} = {2}")
    @DisplayName("calculateGap() returns correct result")
    @CsvSource(
        "3, 4, 0",
        "0, 5, 4",
        "100, 98, -3",
        "0xF1_00_00_00, 0xF1_00_00_02, 1",
        "0x40_00_00_00, 0x80_00_00_01, 0x40_00_00_00",
        "1, 0x40_00_00_02, 0x40_00_00_00",
    )
    fun calculateGap_returnsCorrectResult(previous: Long, current: Long, expected: Int) {
        val actual = calculateGap(previous.toUInt(), current.toUInt())

        assertEquals(expected, actual)
    }
}
