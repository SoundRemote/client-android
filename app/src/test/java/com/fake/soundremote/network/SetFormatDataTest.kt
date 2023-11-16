package com.fake.soundremote.network

import com.fake.soundremote.util.Net
import com.fake.soundremote.util.Net.COMPRESSION_320
import com.fake.soundremote.util.Net.putUByte
import com.fake.soundremote.util.Net.putUShort
import com.fake.soundremote.util.PacketRequestIdType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("SetFormatData")
class SetFormatDataTest {

    @DisplayName("SIZE has correct value")
    @Test
    fun size_ReturnsCorrectValue() {
        val expected = 3

        val actual = SetFormatData.SIZE

        Assertions.assertEquals(expected, actual)
    }

    @DisplayName("write() writes correctly")
    @Test
    fun write_WritesCorrectly() {
        @Net.Compression val compression: Int = COMPRESSION_320
        val requestId: PacketRequestIdType = 0xBCDEu
        val expected = Net.createPacketBuffer(SetFormatData.SIZE)
        expected.putUShort(requestId)
        expected.putUByte(compression.toUByte())
        expected.rewind()

        val actual = Net.createPacketBuffer(SetFormatData.SIZE)
        SetFormatData(compression, requestId).write(actual)
        actual.rewind()

        Assertions.assertEquals(expected, actual)
    }
}
