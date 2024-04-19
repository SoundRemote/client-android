package com.fake.soundremote.ui

import androidx.lifecycle.SavedStateHandle
import com.fake.soundremote.MainDispatcherExtension
import com.fake.soundremote.data.TestHotkeyRepository
import com.fake.soundremote.getHotkey
import com.fake.soundremote.ui.hotkey.HOTKEY_ID_ARG
import com.fake.soundremote.ui.hotkey.HotkeyScreenMode
import com.fake.soundremote.ui.hotkey.HotkeyViewModel
import com.fake.soundremote.util.KeyCode
import com.fake.soundremote.util.KeyGroup
import com.fake.soundremote.util.ModKey
import com.fake.soundremote.util.Mods
import com.fake.soundremote.util.generateDescription
import com.fake.soundremote.util.isModActive
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@ExtendWith(MainDispatcherExtension::class)
@DisplayName("HotkeyViewModel")
class HotkeyViewModelTest {
    private var hotkeyRepository = TestHotkeyRepository()

    private lateinit var viewModel: HotkeyViewModel

    @Nested
    @DisplayName("Create Hotkey mode")
    inner class CreateHotkey {
        @BeforeEach
        fun setup() {
            val savedState = SavedStateHandle(emptyMap())
            viewModel = HotkeyViewModel(savedState, hotkeyRepository)
        }

        @Test
        @DisplayName("Sets screen mode correctly")
        fun screenMode_isCorrect() {
            val actual = viewModel.hotkeyScreenState.value.mode
            assertEquals(HotkeyScreenMode.CREATE, actual)
        }

        @Test
        @DisplayName("updateKeyCode() updates keyCode")
        fun updateKeyCode_updates() {
            val expected = KeyCode(0x60)
            assertNotEquals(expected, viewModel.hotkeyScreenState.value.keyCode)

            viewModel.updateKeyCode(expected)

            val actual = viewModel.hotkeyScreenState.value.keyCode
            assertEquals(expected, actual)
        }

        @Test
        @DisplayName("updateName() updates hotkey name")
        fun updateName_updates() {
            val expected = "HotkeyName"
            assertNotEquals(expected, viewModel.hotkeyScreenState.value.name)

            viewModel.updateName(expected)

            val actual = viewModel.hotkeyScreenState.value.name
            assertEquals(expected, actual)
        }

        @ParameterizedTest
        @EnumSource(ModKey::class)
        @DisplayName("updateMod() updates hotkey mod")
        fun updateMod_updates(mod: ModKey) {
            val modState = when (mod) {
                ModKey.WIN -> viewModel.hotkeyScreenState.value.win
                ModKey.CTRL -> viewModel.hotkeyScreenState.value.ctrl
                ModKey.SHIFT -> viewModel.hotkeyScreenState.value.shift
                ModKey.ALT -> viewModel.hotkeyScreenState.value.alt
            }
            assertFalse(modState)

            viewModel.updateMod(mod, true)

            val actual = when (mod) {
                ModKey.WIN -> viewModel.hotkeyScreenState.value.win
                ModKey.CTRL -> viewModel.hotkeyScreenState.value.ctrl
                ModKey.SHIFT -> viewModel.hotkeyScreenState.value.shift
                ModKey.ALT -> viewModel.hotkeyScreenState.value.alt
            }
            assertTrue(actual)
        }

        @Test
        @DisplayName("canSave() returns false if keyCode is null")
        fun canSave_keyCodeIsNull_returnsFalse() {
            assertNull(viewModel.hotkeyScreenState.value.keyCode)
            assertFalse(viewModel.canSave())
        }

        @Test
        @DisplayName("saveHotkey() saves new Hotkey")
        fun saveHotkey_createsNewHotkey() = runTest {
            val expectedName = "TestName12"
            val expectedKeyCode = KeyCode(0x42)
            viewModel.updateName(expectedName)
            viewModel.updateKeyCode(expectedKeyCode)
            viewModel.updateMod(ModKey.SHIFT, true)
            viewModel.updateMod(ModKey.CTRL, true)
            hotkeyRepository.setHotkeys(emptyList())

            viewModel.saveHotkey("B")

            val savedHotkey = hotkeyRepository.getAllOrdered().firstOrNull()?.firstOrNull()
            assertNotNull(savedHotkey)
            savedHotkey!!
            assertEquals(expectedName, savedHotkey.name)
            assertEquals(expectedKeyCode, savedHotkey.keyCode)
            assertTrue(savedHotkey.isModActive(ModKey.CTRL))
            assertTrue(savedHotkey.isModActive(ModKey.SHIFT))
            assertFalse(savedHotkey.isModActive(ModKey.ALT))
            assertFalse(savedHotkey.isModActive(ModKey.WIN))
        }

        @Test
        @DisplayName("saveHotkey() without name set saves new Hotkey with generated name")
        fun saveHotkey_blankName_createsNewHotkey() = runTest {
            // Letter/digit key
            val expectedKeyCode = KeyCode(0x42)
            val keyLabel = expectedKeyCode.toLetterOrDigitString()!!
            val mods = Mods(win = false, ctrl = true, shift = true, alt = false)
            val expectedName = generateDescription(keyLabel, mods)

            for (mod in ModKey.entries) {
                viewModel.updateMod(mod, mods.isModActive(mod))
            }
            viewModel.updateKeyCode(expectedKeyCode)
            hotkeyRepository.setHotkeys(emptyList())

            viewModel.saveHotkey(keyLabel)

            val savedHotkey = hotkeyRepository.getAllOrdered().firstOrNull()?.firstOrNull()
            assertNotNull(savedHotkey)
            savedHotkey!!
            assertEquals(expectedName, savedHotkey.name)
            assertEquals(expectedKeyCode, savedHotkey.keyCode)
            for (mod in ModKey.entries) {
                val expectedModValue = mods.isModActive(mod)
                val actualModValue = savedHotkey.isModActive(mod)
                assertEquals(expectedModValue, actualModValue)
            }
        }
    }

    @Nested
    @DisplayName("Edit Hotkey mode")
    inner class EditHotkey {
        @Test
        @DisplayName("Sets screen mode correctly")
        fun screenMode_isCorrect() {
            val id = 1
            val hotkey = getHotkey(id = id)
            hotkeyRepository.setHotkeys(listOf(hotkey))
            val savedState = SavedStateHandle(mapOf(HOTKEY_ID_ARG to id))
            viewModel = HotkeyViewModel(savedState, hotkeyRepository)

            val actual = viewModel.hotkeyScreenState.value.mode

            assertEquals(HotkeyScreenMode.EDIT, actual)
        }

        @Test
        @DisplayName("Sets hotkey properties correctly")
        fun hotkeyProperties_areCorrect() {
            val mods = Mods(win = true, ctrl = true, shift = true, alt = true)
            val id = 1
            val hotkey = getHotkey(id = id, mods = mods)
            hotkeyRepository.setHotkeys(listOf(hotkey))
            val savedState = SavedStateHandle(mapOf(HOTKEY_ID_ARG to id))
            viewModel = HotkeyViewModel(savedState, hotkeyRepository)

            val state = viewModel.hotkeyScreenState.value

            assertEquals(hotkey.name, state.name)
            assertEquals(hotkey.keyCode, state.keyCode)
            assertEquals(hotkey.isModActive(ModKey.ALT), state.alt)
            assertEquals(hotkey.isModActive(ModKey.CTRL), state.ctrl)
            assertEquals(hotkey.isModActive(ModKey.SHIFT), state.shift)
            assertEquals(hotkey.isModActive(ModKey.WIN), state.win)
        }

        @ParameterizedTest
        @MethodSource("com.fake.soundremote.ui.HotkeyViewModelTest#keyCodeToGroup")
        @DisplayName("Sets key group correctly")
        fun keyGroup_isCorrect(keyCode: Int, expectedKeyGroup: KeyGroup) {
            val id = 1
            val hotkey = getHotkey(id = id, keyCode = KeyCode(keyCode))
            hotkeyRepository.setHotkeys(listOf(hotkey))
            val savedState = SavedStateHandle(mapOf(HOTKEY_ID_ARG to id))
            viewModel = HotkeyViewModel(savedState, hotkeyRepository)

            val actual = viewModel.hotkeyScreenState.value.keyGroupIndex

            assertEquals(expectedKeyGroup.index, actual)
        }

        @Test
        @DisplayName("saveHotkey() updates Hotkey")
        fun saveHotkey_updatesHotkey() = runTest {
            // Create a Hotkey to edit
            val id = 10
            val hotkey = getHotkey(
                id = id,
                keyCode = KeyCode(0x100),
                mods = Mods(),
                name = "Original name"
            )
            hotkeyRepository.setHotkeys(listOf(hotkey))
            val savedState = SavedStateHandle(mapOf(HOTKEY_ID_ARG to id))
            viewModel = HotkeyViewModel(savedState, hotkeyRepository)
            val expectedName = "New name"
            val expectedKeyCode = KeyCode(0x42)

            viewModel.updateName(expectedName)
            viewModel.updateKeyCode(expectedKeyCode)
            viewModel.updateMod(ModKey.SHIFT, true)
            viewModel.updateMod(ModKey.CTRL, true)
            viewModel.saveHotkey("B")

            val updatedHotkey = hotkeyRepository.getById(id)
            assertNotNull(updatedHotkey)
            updatedHotkey!!
            assertEquals(expectedName, updatedHotkey.name)
            assertEquals(expectedKeyCode, updatedHotkey.keyCode)
            assertTrue(updatedHotkey.isModActive(ModKey.CTRL))
            assertTrue(updatedHotkey.isModActive(ModKey.SHIFT))
            assertFalse(updatedHotkey.isModActive(ModKey.ALT))
            assertFalse(updatedHotkey.isModActive(ModKey.WIN))
        }
    }

    companion object {
        @JvmStatic
        private fun keyCodeToGroup(): Stream<Arguments> {
            return Stream.of(
                arguments(0x30, KeyGroup.LETTER_DIGIT),
                arguments(0x39, KeyGroup.LETTER_DIGIT),
                arguments(0x41, KeyGroup.LETTER_DIGIT),
                arguments(0x5A, KeyGroup.LETTER_DIGIT),
                arguments(0xAD, KeyGroup.MEDIA),
                arguments(0xB3, KeyGroup.MEDIA),
                arguments(0xBA, KeyGroup.TYPING),
                arguments(0xDC, KeyGroup.TYPING),
                arguments(0x09, KeyGroup.CONTROL),
                arguments(0x91, KeyGroup.CONTROL),
                arguments(0x21, KeyGroup.NAVIGATION),
                arguments(0x2E, KeyGroup.NAVIGATION),
                arguments(0x60, KeyGroup.NUM_PAD),
                arguments(0x90, KeyGroup.NUM_PAD),
                arguments(0x70, KeyGroup.FUNCTION),
                arguments(0x7B, KeyGroup.FUNCTION),
            )
        }
    }
}
