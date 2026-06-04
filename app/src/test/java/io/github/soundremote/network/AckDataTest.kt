package io.github.soundremote.network

import io.github.soundremote.util.Net
import io.github.soundremote.util.Net.putUShort
import io.github.soundremote.util.PacketRequestIdType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("AckData")
internal class AckDataTest {

    @DisplayName("Reads correctly")
    @Test
    fun read_ReadsCorrectly() {
        val requestId: PacketRequestIdType = 0xFAAFu
        val customData = 0xFA123456.toInt()
        val expectedCustomData = Net.createPacketBuffer(AckData.CUSTOM_DATA_SIZE)
            .putInt(customData)
        expectedCustomData.rewind()
        val expected = AckData(requestId, expectedCustomData)
        val packet = Net.createPacketBuffer(20)
            .putUShort(requestId)
            .putInt(customData)
        packet.rewind()

        val actual = AckData.read(packet)

        actual shouldBe expected
    }
}