package com.fake.soundremote.ui.hotkey

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.fake.soundremote.R
import com.fake.soundremote.stringResource
import com.fake.soundremote.ui.theme.SoundRemoteTheme
import com.fake.soundremote.util.Key
import com.fake.soundremote.util.KeyCode
import com.fake.soundremote.util.KeyGroup
import com.fake.soundremote.util.ModKey
import com.fake.soundremote.util.toKeyCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

internal class HotkeyScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val navigateUp by composeTestRule.stringResource(R.string.navigate_up)
    private val name by composeTestRule.stringResource(R.string.hotkey_name_edit_label)
    private val keyEdit by composeTestRule.stringResource(R.string.hotkey_key_edit_label)
    private val keyF12 by composeTestRule.stringResource(Key.F12.labelId)

    // Hotkey screen should contain navigate up arrow
    @Test
    fun navigateUp_exist() {
        composeTestRule.setContent {
            CreateHotkeyScreen()
        }

        composeTestRule.onNodeWithContentDescription(navigateUp).assertIsDisplayed()
    }

    // Mods
    @Test
    fun winMod_click_changesWinMod() {
        val initial = true
        val expected = !initial
        var actual: Boolean? = null
        composeTestRule.setContent {
            CreateHotkeyScreen(
                state = HotkeyScreenUIState(win = initial),
                onWinChange = { actual = it },
            )
        }

        composeTestRule.onNodeWithText(ModKey.WIN.label).performClick()

        assertEquals(expected, actual)
    }

    @Test
    fun ctrlMod_click_changesCtrlMod() {
        val initial = true
        val expected = !initial
        var actual: Boolean? = null
        composeTestRule.setContent {
            CreateHotkeyScreen(
                state = HotkeyScreenUIState(ctrl = initial),
                onCtrlChange = { actual = it },
            )
        }

        composeTestRule.onNodeWithText(ModKey.CTRL.label).performClick()

        assertEquals(expected, actual)
    }

    @Test
    fun shiftMod_click_changesShiftMod() {
        val initial = true
        val expected = !initial
        var actual: Boolean? = null
        composeTestRule.setContent {
            CreateHotkeyScreen(
                state = HotkeyScreenUIState(shift = initial),
                onShiftChange = { actual = it },
            )
        }

        composeTestRule.onNodeWithText(ModKey.SHIFT.label).performClick()

        assertEquals(expected, actual)
    }

    @Test
    fun altMod_click_changesAltMod() {
        val initial = true
        val expected = !initial
        var actual: Boolean? = null
        composeTestRule.setContent {
            CreateHotkeyScreen(
                state = HotkeyScreenUIState(alt = initial),
                onAltChange = { actual = it },
            )
        }

        composeTestRule.onNodeWithText(ModKey.ALT.label).performClick()

        assertEquals(expected, actual)
    }

    @Test
    fun nameTextField_textInput_changesName() {
        val expected = "Key Name"
        var actual = ""
        composeTestRule.setContent {
            CreateHotkeyScreen(
                state = HotkeyScreenUIState(),
                onNameChange = { actual = it },
            )
        }

        composeTestRule.onNodeWithText(name).performTextInput(expected)

        assertEquals(expected, actual)
    }

    // Key Groups
    @Test
    fun keyGroupTabs_exist() {
        composeTestRule.setContent {
            CreateHotkeyScreen()
        }

        for (keyGroup in KeyGroup.entries) {
            val groupName = composeTestRule.activity.getString(keyGroup.nameStringId)
            composeTestRule.onNodeWithText(groupName).assertExists()
        }
    }

    // Valid character input (a-z or digit) should call `onKeyCodeChange`
    @Test
    fun keyEdit_validKeyInput_changesKeyCode() {
        val c = 'W'
        val expected = c.toKeyCode()
        var actual: KeyCode? = null
        composeTestRule.setContent {
            CreateHotkeyScreen(
                state = HotkeyScreenUIState(keyGroupIndex = KeyGroup.LETTER_DIGIT.index),
                onKeyCodeChange = { actual = it },
            )
        }

        composeTestRule.onNodeWithText(keyEdit).apply {
            performClick()
            performTextInput(c.toString())
        }

        assertEquals(expected, actual)
    }

    // Invalid character input should not call `onKeyCodeChange`
    @Test
    fun keyEdit_invalidKeyInput_doesNotChangeKeyCode() {
        var actual: KeyCode? = null
        composeTestRule.setContent {
            CreateHotkeyScreen(
                state = HotkeyScreenUIState(keyGroupIndex = KeyGroup.LETTER_DIGIT.index),
                onKeyCodeChange = { actual = it },
            )
        }

        composeTestRule.onNodeWithText(keyEdit).apply {
            performClick()
            performTextInput("@")
        }

        assertNull(actual)
    }

    // Key select menu item click should call `onKeyCodeChange`
    @Test
    fun keySelectMenu_click_changesKeyCode() {
        val expected = Key.F12.keyCode
        var actual: KeyCode? = null
        composeTestRule.setContent {
            CreateHotkeyScreen(
                state = HotkeyScreenUIState(keyGroupIndex = KeyGroup.FUNCTION.index),
                onKeyCodeChange = { actual = it },
            )
        }

        composeTestRule.onNodeWithText(keyEdit).performClick()
        composeTestRule.onNodeWithText(keyF12).apply {
            performScrollTo()
            performClick()
        }

        assertEquals(expected, actual)
    }

    @Suppress("TestFunctionName")
    @Composable
    private fun CreateHotkeyScreen(
        modifier: Modifier = Modifier,
        state: HotkeyScreenUIState = HotkeyScreenUIState(),
        onKeyCodeChange: (KeyCode?) -> Unit = {},
        onWinChange: (Boolean) -> Unit = {},
        onCtrlChange: (Boolean) -> Unit = {},
        onShiftChange: (Boolean) -> Unit = {},
        onAltChange: (Boolean) -> Unit = {},
        onNameChange: (String) -> Unit = {},
        checkCanSave: () -> Boolean = { false },
        onSave: (String) -> Unit = {},
        onClose: () -> Unit = {},
        showSnackbar: (String, SnackbarDuration) -> Unit = { _, _ -> },
        compactHeight: Boolean = false,
    ) {
        SoundRemoteTheme {
            HotkeyScreen(
                state,
                onKeyCodeChange,
                onWinChange,
                onCtrlChange,
                onShiftChange,
                onAltChange,
                onNameChange,
                checkCanSave,
                onSave,
                onClose,
                showSnackbar,
                compactHeight,
                modifier,
            )
        }
    }
}