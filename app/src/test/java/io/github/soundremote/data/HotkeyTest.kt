package io.github.soundremote.data

import io.github.soundremote.getHotkey
import io.github.soundremote.util.ModKey
import io.github.soundremote.util.Mods
import io.github.soundremote.util.isModActive
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
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

            modActive.shouldBeFalse()
        }

        @DisplayName("Are set when created with mods")
        @ParameterizedTest
        @EnumSource(ModKey::class)
        fun isModActive_WithMods_ReturnsTrue(mod: ModKey) {
            val hotkey = getHotkey(mods = Mods(mod.bitField))

            val modActive = hotkey.isModActive(mod)

            modActive.shouldBeTrue()
        }

        @DisplayName("Bitfield is correct set when created without mods")
        @Test
        fun modsBitfield_NoMods_ReturnsCorrectValue() {
            val expected = Mods()
            val hotkey = getHotkey(mods = Mods())

            val actual = hotkey.mods

            actual shouldBe expected
        }

        @DisplayName("Bitfield is correct set when created with Mods")
        @ParameterizedTest
        @EnumSource(ModKey::class)
        fun modsBitfield_WithMod_ReturnsCorrectValue(mod: ModKey) {
            val expected = Mods(mod.bitField)
            val hotkey = getHotkey(mods = Mods(mod.bitField))

            val actual = hotkey.mods

            actual shouldBe expected
        }
    }
}
