package io.github.soundremote.util

import io.github.soundremote.getHotkey
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
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
            ch.toKeyCode() shouldBe KeyCode(expected)
        }

        @ParameterizedTest
        @DisplayName("returns null for a non digit or [a-z/A-Z] letter Char")
        @ValueSource(chars = ['%', 'Ж', '©'])
        fun invalidChar_returnsNull(ch: Char) {
            ch.toKeyCode().shouldBeNull()
        }
    }

    @DisplayName("KeyCode.toLetterOrDigitChar")
    @Nested
    inner class KeyCodeToCharTests {

        @ParameterizedTest
        @DisplayName("returns the correct Char for a digit or [a-z/A-Z] letter key code")
        @CsvSource("48, 0", "57, 9", "65, a", "90, z")
        fun validCode_returnsCorrectChar(code: Int, expected: Char) {
            KeyCode(code).toLetterOrDigitChar() shouldBe expected
        }

        @ParameterizedTest
        @DisplayName("returns null for a non digit or [a-z/A-Z] letter key code")
        @ValueSource(ints = [-1, 0, 200, 500])
        fun invalidCode_returnsNull(code: Int) {
            KeyCode(code).toLetterOrDigitChar().shouldBeNull()
        }
    }

    @DisplayName("KeyCode.toLetterOrDigitString")
    @Nested
    inner class KeyCodeToStringTests {

        @ParameterizedTest
        @DisplayName("returns the correct (uppercase) String for a digit or [a-z/A-Z] letter key code")
        @CsvSource("0x30, 0", "0x39, 9", "0x41, A", "0x5A, Z")
        fun validCode_returnsCorrectChar(code: Int, expected: String) {
            KeyCode(code).toLetterOrDigitString() shouldBe expected
        }

        @ParameterizedTest
        @DisplayName("returns null for a non digit or [a-z/A-Z] letter key code")
        @ValueSource(ints = [-1, 0, 200, 500])
        fun invalidCode_returnsNull(code: Int) {
            KeyCode(code).toLetterOrDigitChar().shouldBeNull()
        }
    }

    @DisplayName("KeyCode.keyLabelId")
    @Nested
    inner class KeyCodeKeyLabelIdTests {

        @ParameterizedTest
        @EnumSource(names = ["MEDIA_VOLUME_UP", "F12", "NUM_ADD"])
        @DisplayName("returns a correct string resource id for a KeyCode associated with a Key entry")
        fun keyInstanceCode_ReturnsCorrectLabelId(key: Key) {
            val expected = key.labelId
            val keyCode = key.keyCode

            keyCode.keyLabelId() shouldBe expected
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

            description.shouldBeInstanceOf<HotkeyDescription.WithString>()
            val actual = description.text.lowercase().contains(mod.label.lowercase())
            actual.shouldBeFalse()
        }

        @ParameterizedTest
        @DisplayName("produces a description with a mod label for a hotkey with mod")
        @EnumSource(ModKey::class)
        fun hotkeyWithOneMod_ContainsCorrectModLabel(mod: ModKey) {
            val hotkey = getHotkey(keyCode = KeyCode('A'.code), mods = Mods(mod.bitField))

            val description = generateDescription(hotkey)

            description.shouldBeInstanceOf<HotkeyDescription.WithString>()
            val actual = description.text.lowercase().contains(mod.label.lowercase())
            actual.shouldBeTrue()
        }

        @ParameterizedTest
        @DisplayName("produces a description with the correct label for a letter/number key code")
        @CsvSource("48, 0", "57, 9", "65, A", "90, z")
        fun hotkey_ContainsCorrectKeyLabel(code: Int, label: String) {
            val hotkey = getHotkey(keyCode = KeyCode(code))

            val description = generateDescription(hotkey)

            description.shouldBeInstanceOf<HotkeyDescription.WithString>()
            val actual = description.text.lowercase().contains(label.lowercase())
            actual.shouldBeTrue()
        }

        @ParameterizedTest
        @EnumSource(names = ["TILDE", "F12", "DELETE"])
        @DisplayName("produces a correct String resource description for a non letter/number key code")
        fun hotkey_ContainsCorrectKeyLabelId(key: Key) {
            val hotkey = getHotkey(keyCode = key.keyCode)

            val description = generateDescription(hotkey)

            description.shouldBeInstanceOf<HotkeyDescription.WithLabelId>()
            description.labelId shouldBe key.labelId
        }
    }
}
