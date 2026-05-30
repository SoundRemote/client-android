package io.github.soundremote.network

import io.github.soundremote.util.Net
import io.github.soundremote.util.Net.putUByte
import io.github.soundremote.util.PacketKeyType
import io.github.soundremote.util.PacketModsType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("HotkeyData")
internal class HotkeyDataTest {

    @DisplayName("SIZE has correct value")
    @Test
    fun size_ReturnsCorrectValue() {
        val expected = 2

        val actual = HotkeyData.SIZE

        actual shouldBe expected
    }

    @DisplayName("write() writes correctly")
    @Test
    fun write_WritesCorrectly() {
        val key: PacketKeyType = 0xDDu
        val mods: PacketModsType = 0x15u
        val expected = Net.createPacketBuffer(HotkeyData.SIZE)
        expected.putUByte(key)
        expected.putUByte(mods)
        expected.rewind()

        val actual = Net.createPacketBuffer(HotkeyData.SIZE)
        HotkeyData(key, mods).write(actual)
        actual.rewind()

        actual shouldBe expected
    }
}
