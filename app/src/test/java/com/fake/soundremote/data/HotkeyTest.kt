package com.fake.soundremote.data

import com.fake.soundremote.getHotkey
import com.fake.soundremote.util.ModKey
import com.fake.soundremote.util.Mods
import com.fake.soundremote.util.isModActive
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@DisplayName("Hotkey")
internal class HotkeyTest {

    @DisplayName("Mods")
    @Nested
    inner class ModsTests {
        @DisplayName("Are not set when created without mods")
        @ParameterizedTest
        @EnumSource(ModKey::class)
        fun isModActive_NoMods_ReturnsFalse(mod: ModKey) {
            val hotkey = getHotkey(mods = Mods())

            val modActive = hotkey.isModActive(mod)

            assertFalse(modActive)
        }

        @DisplayName("Are set when created with mods")
        @ParameterizedTest
        @EnumSource(ModKey::class)
        fun isModActive_WithMods_ReturnsTrue(mod: ModKey) {
            val hotkey = getHotkey(mods = Mods(mod.bitField))

            val modActive = hotkey.isModActive(mod)

            assertTrue(modActive)
        }

        @DisplayName("Bitfield is correct set when created without mods")
        @Test
        fun modsBitfield_NoMods_ReturnsCorrectValue() {
            val expected = Mods()
            val hotkey = getHotkey(mods = Mods())

            val actual = hotkey.mods

            assertEquals(expected, actual)
        }

        @DisplayName("Bitfield is correct set when created with Mods")
        @ParameterizedTest
        @EnumSource(ModKey::class)
        fun modsBitfield_WithMod_ReturnsCorrectValue(mod: ModKey) {
            val expected = Mods(mod.bitField)
            val hotkey = getHotkey(mods = Mods(mod.bitField))

            val actual = hotkey.mods

            assertEquals(expected, actual)
        }
    }
}
