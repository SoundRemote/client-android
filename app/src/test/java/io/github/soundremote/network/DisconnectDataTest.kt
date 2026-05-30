package io.github.soundremote.network

import io.github.soundremote.util.Net
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("DisconnectData")
internal class DisconnectDataTest {

    @DisplayName("SIZE has correct value")
    @Test
    fun size_ReturnsCorrectValue() {
        val expected = 0

        val actual = DisconnectData.SIZE

        actual shouldBe expected
    }

    @DisplayName("write() writes correctly")
    @Test
    fun write_WritesCorrectly() {
        val disconnectData = DisconnectData()
        val expected = Net.createPacketBuffer(DisconnectData.SIZE)
        expected.rewind()

        val actual = Net.createPacketBuffer(DisconnectData.SIZE)
        disconnectData.write(actual)
        actual.rewind()

        actual shouldBe expected
    }
}
