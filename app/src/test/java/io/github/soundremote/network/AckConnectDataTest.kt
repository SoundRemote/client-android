package io.github.soundremote.network

import io.github.soundremote.util.Net
import io.github.soundremote.util.Net.putUByte
import io.github.soundremote.util.PacketProtocolType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("AckConnectData")
internal class AckConnectDataTest {

    @DisplayName("Reads correctly")
    @Test
    fun read_ReadsCorrectly() {
        val expected: PacketProtocolType = 0xFDu
        val buffer = Net.createPacketBuffer(AckData.CUSTOM_DATA_SIZE)
            .putUByte(expected)
        buffer.rewind()

        val ackConnectData = AckConnectData.read(buffer)
        val actual = ackConnectData?.protocol

        actual shouldBe expected
    }
}
