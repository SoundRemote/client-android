package com.fake.soundremote.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.fake.soundremote.R
import com.fake.soundremote.stringResource
import com.fake.soundremote.ui.theme.SoundRemoteTheme
import com.fake.soundremote.util.HotkeyDescription
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HotkeySelectDialogTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val dialogTitle by composeTestRule.stringResource(R.string.hotkey_select_title)
    private val noHotkey by composeTestRule.stringResource(R.string.hotkey_select_none)
    private val ok by composeTestRule.stringResource(android.R.string.ok)
    private val cancel by composeTestRule.stringResource(R.string.cancel)

    // "No hotkey" option should be displayed
    @Test
    fun noHotkeyOption_isDisplayed() {
        composeTestRule.setContent {
            CreateHotkeySelectDialog()
        }

        composeTestRule.onNodeWithText(noHotkey).assertIsDisplayed()
    }

    // "No hotkey" option should be selected if there are no other options
    @Test
    fun noHotkeyOption_noHotkeys_isSelected() {
        composeTestRule.setContent {
            CreateHotkeySelectDialog()
        }

        composeTestRule.onNodeWithText(noHotkey).assertIsSelected()
    }

    // "No hotkey" option should be selected when `initialHotkeyId` argument is null
    @Test
    fun noHotkeyOption_initialIdIsNull_isSelected() {
        val items = buildList {
            repeat(3) {
                val id = it + 1
                val desc = HotkeyDescription.WithString("Desc $id")
                add(HotkeyInfoUIState(id, "Key $id", desc))
            }
        }
        composeTestRule.setContent {
            CreateHotkeySelectDialog(items = items, initialHotkeyId = null)
        }

        composeTestRule.onNodeWithText(noHotkey).assertIsSelected()
    }

    // Given: a long list of hotkeys that doesn't fit into screen and initially selected
    // hotkey id at the end of a that list.
    // Expected: dialog should scroll to the selected hotkey.
    @Test
    fun hotkeyOption_needsScrolling_isSelected() {
        val count = 100
        val items = buildList {
            repeat(count) {
                val id = it + 1
                val desc = HotkeyDescription.WithString("Desc $id")
                add(HotkeyInfoUIState(id, "Key $id", desc))
            }
        }
        composeTestRule.setContent {
            CreateHotkeySelectDialog(items = items, initialHotkeyId = count)
        }

        composeTestRule.onNodeWithText("Key $count").apply {
            assertIsDisplayed()
            assertIsSelected()
        }
    }

    @Test
    fun cancelButton_onClick_dismisses() {
        var dismissed = false
        composeTestRule.setContent {
            CreateHotkeySelectDialog(onDismiss = { dismissed = true })
        }

        composeTestRule.onNodeWithText(cancel).performClick()

        assertTrue(dismissed)
    }

    @Test
    fun okButton_onClick_confirmsWithCorrectId() {
        val count = 3
        var actual: Int? = -1
        composeTestRule.setContent {
            val items = buildList {
                repeat(count) {
                    val id = it + 1
                    val desc = HotkeyDescription.WithString("Desc $id")
                    add(HotkeyInfoUIState(id, "Key $id", desc))
                }
            }
            CreateHotkeySelectDialog(
                items = items,
                initialHotkeyId = 1,
                onConfirm = { actual = it },
            )
        }

        composeTestRule.apply {
            onNodeWithText("Key $count").performClick()
            onNodeWithText(ok).performClick()
        }

        assertEquals(count, actual)
    }

    @Suppress("TestFunctionName")
    @Composable
    private fun CreateHotkeySelectDialog(
        title: String = dialogTitle,
        items: List<HotkeyInfoUIState> = emptyList(),
        initialHotkeyId: Int? = null,
        onConfirm: (Int?) -> Unit = {},
        onDismiss: () -> Unit = {},
    ) {
        SoundRemoteTheme {
            HotkeySelectDialog(
                title = title,
                items = items,
                initialHotkeyId = initialHotkeyId,
                onConfirm = onConfirm,
                onDismiss = onDismiss,
            )
        }
    }
}