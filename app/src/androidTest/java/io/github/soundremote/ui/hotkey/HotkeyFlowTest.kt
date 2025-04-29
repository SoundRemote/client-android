package io.github.soundremote.ui.hotkey

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.soundremote.MainActivity
import io.github.soundremote.R
import io.github.soundremote.stringResource
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@HiltAndroidTest
class HotkeyFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @BindValue
    @get:Rule(order = 1)
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val editHotkeys by composeTestRule.stringResource(R.string.action_edit_hotkeys)
    private val createHotkey by composeTestRule.stringResource(R.string.action_hotkey_create)
    private val altMod by composeTestRule.stringResource(R.string.alt_checkbox_label)
    private val ctrlMod by composeTestRule.stringResource(R.string.ctrl_checkbox_label)
    private val hotkeyEdit by composeTestRule.stringResource(R.string.hotkey_key_edit_label)
    private val save by composeTestRule.stringResource(R.string.save)
    private val name by composeTestRule.stringResource(R.string.hotkey_name_edit_label)
    private val clear by composeTestRule.stringResource(R.string.clear)

    @Test
    fun createAndEditHotkeyFlow() {
        // Go to Create Hotkey screen
        composeTestRule.apply {
            onNodeWithContentDescription(editHotkeys).performClick()
            onNodeWithContentDescription(createHotkey).performClick()
        }

        // Create hotkey
        val originalName = "My name"
        val originalKey = "y"
        composeTestRule.apply {
            onNodeWithText(hotkeyEdit).performClick()
            onNodeWithText(hotkeyEdit).performTextInput(originalKey)
            onNodeWithText(altMod).performScrollTo()
            onNodeWithText(altMod).performClick()
            onNodeWithText(name).performTextInput(originalName)
            onNodeWithContentDescription(save).performClick()
        }

        // Assert created
        composeTestRule.onNodeWithText(originalName).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            text = altMod,
            substring = true,
            ignoreCase = true,
        ).assertIsDisplayed()

        // Edit
        val editedName = "New name"
        val editedKey = "m"
        composeTestRule.apply {
            onNodeWithText(originalName).performClick()
            onNodeWithText(hotkeyEdit).performClick()
            onNodeWithText(hotkeyEdit).performTextInput(editedKey)
            onNodeWithText(altMod).performScrollTo()
            onNodeWithText(altMod).performClick()
            onNodeWithText(ctrlMod).performScrollTo()
            onNodeWithText(ctrlMod).performClick()
            onNodeWithContentDescription(clear).performClick()
            onNodeWithText(name).performTextInput(editedName)
            onNodeWithContentDescription(save).performClick()
        }

        // Assert edited
        composeTestRule.onNodeWithText(editedName).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            text = ctrlMod,
            substring = true,
            ignoreCase = true,
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            text = altMod,
            substring = true,
            ignoreCase = true,
        ).assertIsNotDisplayed()
    }
}
