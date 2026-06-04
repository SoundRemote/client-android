package io.github.soundremote.network

import io.github.soundremote.util.Net
import io.github.soundremote.util.Net.COMPRESSION_320
import io.github.soundremote.util.Net.putUByte
import io.github.soundremote.util.Net.putUShort
import io.github.soundremote.util.PacketRequestIdType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ConnectData")
internal class ConnectDataTest {

    @DisplayName("SIZE has correct value")
    @Test
    fun size_ReturnsCorrectValue() {
        val expected = 4

        val actual = ConnectData.SIZE

        actual shouldBe expected
    }

    @DisplayName("Writes correctly")
    @Test
    fun write_WritesCorrectly() {
        val requestId: PacketRequestIdType = 0xFAAFu
        @Net.Compression val compression = COMPRESSION_320
        val expected = Net.createPacketBuffer(ConnectData.SIZE)
        expected.putUByte(Net.PROTOCOL_VERSION)
        expected.putUShort(requestId)
        expected.putUByte(compression.toUByte())
        expected.rewind()

        val actual = Net.createPacketBuffer(ConnectData.SIZE)
        ConnectData(compression, requestId).write(actual)
        actual.rewind()

        actual shouldBe expected
    }
}
