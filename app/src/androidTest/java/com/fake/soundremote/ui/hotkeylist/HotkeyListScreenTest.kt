package com.fake.soundremote.ui.hotkeylist

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.fake.soundremote.R
import com.fake.soundremote.stringResource
import com.fake.soundremote.ui.theme.SoundRemoteTheme
import com.fake.soundremote.util.HotkeyDescription
import com.fake.soundremote.util.TestTag
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

internal class HotkeyListScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val navigateUp by composeTestRule.stringResource(R.string.navigate_up)
    private val hotkeyActionsMenu by composeTestRule.stringResource(R.string.hotkey_actions_menu_description)
    private val actionEdit by composeTestRule.stringResource(R.string.edit)
    private val actionDelete by composeTestRule.stringResource(R.string.delete)
    private val deleteConfirmationTemplate by composeTestRule.stringResource(R.string.hotkey_delete_confirmation)

    // Hotkey list screen should contain navigate up arrow
    @Test
    fun navigateUp_exist() {
        composeTestRule.setContent {
            CreateHotkeyListScreen()
        }

        composeTestRule.onNodeWithContentDescription(navigateUp).assertIsDisplayed()
    }

    // Hotkey name and description are displayed
    @Test
    fun hotkey_isDisplayed() {
        val name = "Test name"
        val desc = "Test description"
        val hotkeyState = HotkeyUIState(1, name, HotkeyDescription.WithString(desc), false)
        composeTestRule.setContent {
            CreateHotkeyListScreen(state = HotkeyListUIState(listOf(hotkeyState)))
        }

        composeTestRule.onNodeWithText(name).assertIsDisplayed()
        composeTestRule.onNodeWithText(desc).assertIsDisplayed()
    }

    // Hotkey favoured status switch is displayed
    @Test
    fun favouredSwitch_isDisplayed() {
        val name = "Test name"
        val desc = HotkeyDescription.WithString("Test description")
        val hotkeyState = HotkeyUIState(1, name, desc, true)
        composeTestRule.setContent {
            CreateHotkeyListScreen(state = HotkeyListUIState(listOf(hotkeyState)))
        }

        composeTestRule.onNodeWithTag(TestTag.FAVOURITE_SWITCH).assertIsDisplayed()
    }

    // Hotkey favoured status switch is on for a favoured hotkey
    @Test
    fun favouredSwitch_hotkeyIsFavoured_isOn() {
        val name = "Test name"
        val desc = HotkeyDescription.WithString("Test description")
        val hotkeyState = HotkeyUIState(1, name, desc, true)
        composeTestRule.setContent {
            CreateHotkeyListScreen(state = HotkeyListUIState(listOf(hotkeyState)))
        }

        composeTestRule.onNodeWithTag(TestTag.FAVOURITE_SWITCH).assertIsOn()
    }

    // Hotkey favoured status switch is off for an unfavoured hotkey
    @Test
    fun favouredSwitch_hotkeyIsNotFavoured_isOff() {
        val name = "Test name"
        val desc = HotkeyDescription.WithString("Test description")
        val hotkeyState = HotkeyUIState(1, name, desc, false)
        composeTestRule.setContent {
            CreateHotkeyListScreen(state = HotkeyListUIState(listOf(hotkeyState)))
        }

        composeTestRule.onNodeWithTag(TestTag.FAVOURITE_SWITCH).assertIsOff()
    }

    // Hotkey actions menu is displayed on actions menu button click
    @Test
    fun hotkeyActionsButton_onClick_displaysMenu() {
        val desc = HotkeyDescription.WithString("Desc")
        val hotkeyState = HotkeyUIState(1, "Name", desc, false)
        composeTestRule.setContent {
            CreateHotkeyListScreen(state = HotkeyListUIState(listOf(hotkeyState)))
        }

        composeTestRule.apply {
            onNodeWithContentDescription(hotkeyActionsMenu).performClick()
            onNodeWithText(actionEdit).assertIsDisplayed()
            onNodeWithText(actionDelete).assertIsDisplayed()
        }
    }

    // Hotkey edit action calls "onEdit" with correct id
    @Test
    fun hotkeyActionEdit_onClick_callsEdit() {
        val id = 42
        val desc = HotkeyDescription.WithString("Desc")
        val hotkeyState = HotkeyUIState(id, "Name", desc, false)
        var actualId = -1
        composeTestRule.setContent {
            CreateHotkeyListScreen(
                state = HotkeyListUIState(listOf(hotkeyState)),
                onEdit = { actualId = id },
            )
        }

        composeTestRule.onNodeWithContentDescription(hotkeyActionsMenu).performClick()
        composeTestRule.onNodeWithText(actionEdit).performClick()

        assertEquals(id, actualId)
    }

    // Hotkey delete action shows confirmation dialog
    @Test
    fun hotkeyActionDelete_onClick_showsConfirmationDialog() {
        val name = "Test name"
        val desc = HotkeyDescription.WithString("Desc")
        val hotkeyState = HotkeyUIState(1, name, desc, false)
        composeTestRule.setContent {
            CreateHotkeyListScreen(
                state = HotkeyListUIState(listOf(hotkeyState)),
            )
        }

        composeTestRule.apply {
            onNodeWithContentDescription(hotkeyActionsMenu).performClick()
            onNodeWithText(actionDelete).performClick()

            val confirmationText = deleteConfirmationTemplate.format(name)
            onNodeWithText(confirmationText).assertIsDisplayed()
        }
    }

    // Clicking Delete button in delete confirmation dialog calls "onDelete"
    @Test
    fun deleteConfirmationDialog_onClickDelete_callsDelete() {
        val id = 42
        val desc = HotkeyDescription.WithString("Desc")
        val hotkeyState = HotkeyUIState(id, "Test name", desc, false)
        var actualId = -1
        composeTestRule.setContent {
            CreateHotkeyListScreen(
                state = HotkeyListUIState(listOf(hotkeyState)),
                onDelete = { actualId = id },
            )
        }

        composeTestRule.apply {
            onNodeWithContentDescription(hotkeyActionsMenu).performClick()
            // Menu - Delete
            onNodeWithText(actionDelete).performClick()
            // Confirmation dialog - Delete button
            onNodeWithText(actionDelete).performClick()
        }

        assertEquals(id, actualId)
    }

    // Click on Hotkey calls "onEdit" function with correct id
    @Test
    fun hotkey_onClick_callsEdit() {
        val id = 42
        val name = "Test name"
        val desc = HotkeyDescription.WithString("Desc")
        val hotkeyState = HotkeyUIState(id, name, desc, false)
        var actualId = -1
        composeTestRule.setContent {
            CreateHotkeyListScreen(
                state = HotkeyListUIState(listOf(hotkeyState)),
                onEdit = { actualId = id },
            )
        }

        composeTestRule.onNodeWithText(name).performClick()

        assertEquals(id, actualId)
    }

    @Suppress("TestFunctionName")
    @Composable
    private fun CreateHotkeyListScreen(
        modifier: Modifier = Modifier,
        state: HotkeyListUIState = HotkeyListUIState(),
        onCreate: () -> Unit = {},
        onEdit: (id: Int) -> Unit = {},
        onDelete: (id: Int) -> Unit = {},
        onChangeFavoured: (id: Int, favoured: Boolean) -> Unit = { _, _ -> },
        onMove: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
        onNavigateUp: () -> Unit = {},
    ) {
        SoundRemoteTheme {
            HotkeyListScreen(
                state,
                onNavigateToHotkeyCreate = onCreate,
                onNavigateToHotkeyEdit = onEdit,
                onDelete = onDelete,
                onChangeFavoured = onChangeFavoured,
                onMove = onMove,
                onNavigateUp = onNavigateUp,
                modifier = modifier,
            )
        }
    }
}
