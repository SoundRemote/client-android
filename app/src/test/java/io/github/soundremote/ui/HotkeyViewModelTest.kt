package io.github.soundremote.ui

import io.github.soundremote.MainDispatcherExtension
import io.github.soundremote.data.TestHotkeyRepository
import io.github.soundremote.getHotkey
import io.github.soundremote.ui.hotkey.HotkeyScreenMode
import io.github.soundremote.ui.hotkey.HotkeyViewModel
import io.github.soundremote.util.KeyCode
import io.github.soundremote.util.KeyGroup
import io.github.soundremote.util.ModKey
import io.github.soundremote.util.Mods
import io.github.soundremote.util.generateDescription
import io.github.soundremote.util.isModActive
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
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
            viewModel = HotkeyViewModel(hotkeyRepository)
        }

        @Test
        @DisplayName("Sets screen mode correctly")
        fun screenMode_isCorrect() {
            val actual = viewModel.hotkeyScreenState.value.mode

            actual shouldBe HotkeyScreenMode.CREATE
        }

        @Test
        @DisplayName("updateKeyCode() updates keyCode")
        fun updateKeyCode_updates() {
            val expected = KeyCode(0x60)
            viewModel.hotkeyScreenState.value.keyCode shouldNotBe expected

            viewModel.updateKeyCode(expected)

            viewModel.hotkeyScreenState.value.keyCode shouldBe expected
        }

        @Test
        @DisplayName("updateName() updates hotkey name")
        fun updateName_updates() {
            val expected = "HotkeyName"
            viewModel.hotkeyScreenState.value.name shouldNotBe expected

            viewModel.updateName(expected)

            viewModel.hotkeyScreenState.value.name shouldBe expected
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
            modState.shouldBeFalse()

            viewModel.updateMod(mod, true)

            val actual = when (mod) {
                ModKey.WIN -> viewModel.hotkeyScreenState.value.win
                ModKey.CTRL -> viewModel.hotkeyScreenState.value.ctrl
                ModKey.SHIFT -> viewModel.hotkeyScreenState.value.shift
                ModKey.ALT -> viewModel.hotkeyScreenState.value.alt
            }
            actual.shouldBeTrue()
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
            savedHotkey.shouldNotBeNull()
            savedHotkey.name shouldBe expectedName
            savedHotkey.keyCode shouldBe expectedKeyCode
            savedHotkey.isModActive(ModKey.CTRL).shouldBeTrue()
            savedHotkey.isModActive(ModKey.SHIFT).shouldBeTrue()
            savedHotkey.isModActive(ModKey.ALT).shouldBeFalse()
            savedHotkey.isModActive(ModKey.WIN).shouldBeFalse()
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
            savedHotkey.shouldNotBeNull()
            savedHotkey.name shouldBe expectedName
            savedHotkey.keyCode shouldBe expectedKeyCode
            for (mod in ModKey.entries) {
                val expectedModValue = mods.isModActive(mod)
                val actualModValue = savedHotkey.isModActive(mod)
                actualModValue shouldBe expectedModValue
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
            viewModel = HotkeyViewModel(hotkeyRepository)
            viewModel.loadHotkey(id)

            viewModel.hotkeyScreenState.value.mode shouldBe HotkeyScreenMode.EDIT
        }

        @Test
        @DisplayName("Sets hotkey properties correctly")
        fun hotkeyProperties_areCorrect() {
            val mods = Mods(win = true, ctrl = true, shift = true, alt = true)
            val id = 1
            val hotkey = getHotkey(id = id, mods = mods)
            hotkeyRepository.setHotkeys(listOf(hotkey))
            viewModel = HotkeyViewModel(hotkeyRepository)
            viewModel.loadHotkey(id)

            val state = viewModel.hotkeyScreenState.value

            state.name shouldBe hotkey.name
            state.keyCode shouldBe hotkey.keyCode
            state.alt shouldBe hotkey.isModActive(ModKey.ALT)
            state.ctrl shouldBe hotkey.isModActive(ModKey.CTRL)
            state.shift shouldBe hotkey.isModActive(ModKey.SHIFT)
            state.win shouldBe hotkey.isModActive(ModKey.WIN)
        }

        @ParameterizedTest
        @MethodSource("io.github.soundremote.ui.HotkeyViewModelTest#keyCodeToGroup")
        @DisplayName("Sets key group correctly")
        fun keyGroup_isCorrect(keyCode: Int, expectedKeyGroup: KeyGroup) {
            val id = 1
            val hotkey = getHotkey(id = id, keyCode = KeyCode(keyCode))
            hotkeyRepository.setHotkeys(listOf(hotkey))
            viewModel = HotkeyViewModel(hotkeyRepository)
            viewModel.loadHotkey(id)

            viewModel.hotkeyScreenState.value.keyGroupIndex shouldBe expectedKeyGroup.index
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
            viewModel = HotkeyViewModel(hotkeyRepository)
            viewModel.loadHotkey(id)
            val expectedName = "New name"
            val expectedKeyCode = KeyCode(0x42)

            viewModel.updateName(expectedName)
            viewModel.updateKeyCode(expectedKeyCode)
            viewModel.updateMod(ModKey.SHIFT, true)
            viewModel.updateMod(ModKey.CTRL, true)
            viewModel.saveHotkey("B")

            val updatedHotkey = hotkeyRepository.getById(id)
            updatedHotkey.shouldNotBeNull()
            updatedHotkey.name shouldBe expectedName
            updatedHotkey.keyCode shouldBe expectedKeyCode
            updatedHotkey.isModActive(ModKey.CTRL).shouldBeTrue()
            updatedHotkey.isModActive(ModKey.SHIFT).shouldBeTrue()
            updatedHotkey.isModActive(ModKey.ALT).shouldBeFalse()
            updatedHotkey.isModActive(ModKey.WIN).shouldBeFalse()
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
