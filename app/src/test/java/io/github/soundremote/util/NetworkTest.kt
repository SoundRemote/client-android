package io.github.soundremote.util

import io.github.soundremote.network.ConnectData
import io.github.soundremote.network.DisconnectData
import io.github.soundremote.network.HotkeyData
import io.github.soundremote.network.KeepAliveData
import io.github.soundremote.network.PacketHeader
import io.github.soundremote.network.SetFormatData
import io.github.soundremote.util.Net.COMPRESSION_256
import io.github.soundremote.util.Net.COMPRESSION_320
import io.github.soundremote.util.Net.PROTOCOL_VERSION
import io.github.soundremote.util.Net.calculateGap
import io.github.soundremote.util.Net.getConnectPacket
import io.github.soundremote.util.Net.getDisconnectPacket
import io.github.soundremote.util.Net.getHotkeyPacket
import io.github.soundremote.util.Net.getKeepAlivePacket
import io.github.soundremote.util.Net.getSetFormatPacket
import io.github.soundremote.util.Net.uByte
import io.github.soundremote.util.Net.uInt
import io.github.soundremote.util.Net.uShort
import io.kotest.matchers.shouldBe
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

            buf.uByte shouldBe expected
        }

        @Test
        @DisplayName("increments the ByteBuffer position by 1")
        fun incrementsPositionCorrectly() {
            val buf = ByteBuffer.allocate(8)

            buf.uByte

            buf.position() shouldBe 1
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

            buf.uShort shouldBe expected
        }

        @Test
        @DisplayName("increments the ByteBuffer position by 2")
        fun incrementsPositionCorrectly() {
            val buf = ByteBuffer.allocate(8)

            buf.uShort

            buf.position() shouldBe 2
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

            buf.uInt shouldBe expected
        }

        @Test
        @DisplayName("increments the ByteBuffer position by 4")
        fun incrementsPositionCorrectly() {
            val buf = ByteBuffer.allocate(8)

            buf.uInt

            buf.position() shouldBe 4
        }
    }

    @Test
    @DisplayName("getConnectPacket() returns correct packet")
    fun getConnectPacket_returnsCorrectPacket() {
        @Net.Compression val compression = COMPRESSION_320
        val requestId: PacketRequestIdType = 0xf123u
        val expectedSize = PacketHeader.SIZE + ConnectData.SIZE

        val actual = getConnectPacket(compression, requestId)

        actual.remaining() shouldBe expectedSize
        actual.uShort shouldBe Net.PROTOCOL_SIGNATURE
        actual.uByte shouldBe Net.PacketCategory.CONNECT.value
        actual.uShort shouldBe expectedSize.toUShort()
        actual.uByte shouldBe PROTOCOL_VERSION
        actual.uShort shouldBe requestId
        actual.uByte shouldBe compression.toUByte()
    }

    @Test
    @DisplayName("getSetFormatPacket() returns correct packet")
    fun getSetFormatPacket_returnsCorrectPacket() {
        @Net.Compression val compression = COMPRESSION_256
        val requestId: PacketRequestIdType = 0xfacbu
        val expectedSize = PacketHeader.SIZE + SetFormatData.SIZE

        val actual = getSetFormatPacket(compression, requestId)

        actual.remaining() shouldBe expectedSize
        actual.uShort shouldBe Net.PROTOCOL_SIGNATURE
        actual.uByte shouldBe Net.PacketCategory.SET_FORMAT.value
        actual.uShort shouldBe expectedSize.toUShort()
        actual.uShort shouldBe requestId
        actual.uByte shouldBe compression.toUByte()
    }

    @Test
    @DisplayName("getHotkeyPacket() returns correct packet")
    fun getHotkeyPacket_returnsCorrectPacket() {
        val keyCode: PacketKeyType = 0xfbu
        val mods: PacketModsType = 0xfau
        val expectedSize = PacketHeader.SIZE + HotkeyData.SIZE

        val actual = getHotkeyPacket(keyCode, mods)

        actual.remaining() shouldBe expectedSize
        actual.uShort shouldBe Net.PROTOCOL_SIGNATURE
        actual.uByte shouldBe Net.PacketCategory.HOTKEY.value
        actual.uShort shouldBe expectedSize.toUShort()
        actual.uByte shouldBe keyCode
        actual.uByte shouldBe mods
    }

    @Test
    @DisplayName("getKeepAlivePacket() returns correct packet")
    fun getKeepAlivePacket_returnsCorrectPacket() {
        val expectedSize = PacketHeader.SIZE + KeepAliveData.SIZE

        val actual = getKeepAlivePacket()

        actual.remaining() shouldBe expectedSize
        actual.uShort shouldBe Net.PROTOCOL_SIGNATURE
        actual.uByte shouldBe Net.PacketCategory.CLIENT_KEEP_ALIVE.value
        actual.uShort shouldBe expectedSize.toUShort()
    }

    @Test
    @DisplayName("getDisconnectPacket() returns correct packet")
    fun getDisconnectPacket_returnsCorrectPacket() {
        val expectedSize = PacketHeader.SIZE + DisconnectData.SIZE

        val actual = getDisconnectPacket()

        actual.remaining() shouldBe expectedSize
        actual.uShort shouldBe Net.PROTOCOL_SIGNATURE
        actual.uByte shouldBe Net.PacketCategory.DISCONNECT.value
        actual.uShort shouldBe expectedSize.toUShort()
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
        calculateGap(previous.toUInt(), current.toUInt()) shouldBe expected
    }
}
