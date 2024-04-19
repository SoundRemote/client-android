package com.fake.soundremote.util

import com.fake.soundremote.getHotkey
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("KeyUtils")
internal class KeysTest {

    @DisplayName("Char.toKeyCode")
    @Nested
    inner class ToKeyCodeTests {
        @ParameterizedTest
        @DisplayName("returns the correct code for a digit or [a-z/A-Z] letter Char")
        @CsvSource("0, 48", "9, 57", "a, 65", "A, 65", "z, 90", "Z, 90")
        fun validChar_returnsCorrectCode(ch: Char, expected: Int) {
            val actual = ch.toKeyCode()

            assertEquals(KeyCode(expected), actual)
        }

        @ParameterizedTest
        @DisplayName("returns null for a non digit or [a-z/A-Z] letter Char")
        @ValueSource(chars = ['%', 'Ж', '©'])
        fun invalidChar_returnsNull(ch: Char) {
            val actual = ch.toKeyCode()

            assertNull(actual)
        }
    }

    @DisplayName("KeyCode.toLetterOrDigitChar")
    @Nested
    inner class KeyCodeToCharTests {
        @ParameterizedTest
        @DisplayName("returns the correct Char for a digit or [a-z/A-Z] letter key code")
        @CsvSource("48, 0", "57, 9", "65, a", "90, z")
        fun validCode_returnsCorrectChar(code: Int, expected: Char) {
            val actual = KeyCode(code).toLetterOrDigitChar()

            assertEquals(expected, actual)
        }

        @ParameterizedTest
        @DisplayName("returns null for a non digit or [a-z/A-Z] letter key code")
        @ValueSource(ints = [-1, 0, 200, 500])
        fun invalidCode_returnsNull(code: Int) {
            val actual = KeyCode(code).toLetterOrDigitChar()

            assertNull(actual)
        }
    }

    @DisplayName("KeyCode.toLetterOrDigitString")
    @Nested
    inner class KeyCodeToStringTests {
        @ParameterizedTest
        @DisplayName("returns the correct (uppercase) String for a digit or [a-z/A-Z] letter key code")
        @CsvSource("0x30, 0", "0x39, 9", "0x41, A", "0x5A, Z")
        fun validCode_returnsCorrectChar(code: Int, expected: String) {
            val actual = KeyCode(code).toLetterOrDigitString()

            assertEquals(expected, actual)
        }

        @ParameterizedTest
        @DisplayName("returns null for a non digit or [a-z/A-Z] letter key code")
        @ValueSource(ints = [-1, 0, 200, 500])
        fun invalidCode_returnsNull(code: Int) {
            val actual = KeyCode(code).toLetterOrDigitChar()

            assertNull(actual)
        }
    }

    @DisplayName("generateDescription")
    @Nested
    inner class GenerateDescriptionTests {
        @ParameterizedTest
        @DisplayName("produces a description without mod key labels for a hotkey without mods")
        @EnumSource(ModKey::class)
        fun hotkeyWithoutMods_ContainsNoModLabel(mod: ModKey) {
            val hotkey = getHotkey(keyCode = KeyCode('A'.code), mods = Mods())

            val description = generateDescription(hotkey)

            assertTrue(description is HotkeyDescription.WithString)
            description as HotkeyDescription.WithString
            val actual = description.text.lowercase().contains(mod.label.lowercase())
            assertFalse(actual)
        }

        @ParameterizedTest
        @DisplayName("produces a description with a mod label for a hotkey with mod")
        @EnumSource(ModKey::class)
        fun hotkeyWithOneMod_ContainsCorrectModLabel(mod: ModKey) {
            val hotkey = getHotkey(keyCode = KeyCode('A'.code), mods = Mods(mod.bitField))

            val description = generateDescription(hotkey)

            assertTrue(description is HotkeyDescription.WithString)
            description as HotkeyDescription.WithString
            val actual = description.text.lowercase().contains(mod.label.lowercase())
            assertTrue(actual)
        }

        @ParameterizedTest
        @DisplayName("produces a description with the correct label for a letter/number key code")
        @CsvSource("48, 0", "57, 9", "65, A", "90, z")
        fun hotkey_ContainsCorrectKeyLabel(code: Int, label: String) {
            val hotkey = getHotkey(keyCode = KeyCode(code))

            val description = generateDescription(hotkey)

            assertTrue(description is HotkeyDescription.WithString)
            description as HotkeyDescription.WithString
            val actual = description.text.lowercase().contains(label.lowercase())
            assertTrue(actual)
        }

        @ParameterizedTest
        @EnumSource(names = ["TILDE", "F12", "DELETE"])
        @DisplayName("produces a correct String resource description for a non letter/number key code")
        fun hotkey_ContainsCorrectKeyLabelId(key: Key) {
            val hotkey = getHotkey(keyCode = key.keyCode)

            val description = generateDescription(hotkey)

            assertTrue(description is HotkeyDescription.WithLabelId)
            description as HotkeyDescription.WithLabelId
            assertEquals(key.labelId, description.labelId)
        }
    }
}
