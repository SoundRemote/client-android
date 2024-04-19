package com.fake.soundremote.network

import com.fake.soundremote.util.Net
import com.fake.soundremote.util.Net.putUByte
import com.fake.soundremote.util.PacketKeyType
import com.fake.soundremote.util.PacketModsType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("HotkeyData")
internal class HotkeyDataTest {

    @DisplayName("SIZE has correct value")
    @Test
    fun size_ReturnsCorrectValue() {
        val expected = 2

        val actual = HotkeyData.SIZE

        Assertions.assertEquals(expected, actual)
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

        Assertions.assertEquals(expected, actual)
    }
}
