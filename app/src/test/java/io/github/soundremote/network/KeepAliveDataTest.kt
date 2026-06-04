package io.github.soundremote.network

import io.github.soundremote.util.Net
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("KeepAliveData")
class KeepAliveDataTest {

    @DisplayName("SIZE has correct value")
    @Test
    fun size_ReturnsCorrectValue() {
        val expected = 0

        val actual = KeepAliveData.SIZE

        actual shouldBe expected
    }

    @DisplayName("write() writes correctly")
    @Test
    fun write_WritesCorrectly() {
        val keepAliveData = KeepAliveData()
        val expected = Net.createPacketBuffer(KeepAliveData.SIZE)
        expected.rewind()

        val actual = Net.createPacketBuffer(KeepAliveData.SIZE)
        keepAliveData.write(actual)
        actual.rewind()

        actual shouldBe expected
    }
}
