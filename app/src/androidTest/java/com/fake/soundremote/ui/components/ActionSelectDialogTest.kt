package com.fake.soundremote.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.fake.soundremote.R
import com.fake.soundremote.data.Action
import com.fake.soundremote.data.ActionType
import com.fake.soundremote.data.AppAction
import com.fake.soundremote.stringResource
import com.fake.soundremote.ui.theme.SoundRemoteTheme
import com.fake.soundremote.util.HotkeyDescription
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class ActionSelectDialogTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val ok by composeTestRule.stringResource(android.R.string.ok)
    private val cancel by composeTestRule.stringResource(R.string.cancel)
    private val noAction by composeTestRule.stringResource(R.string.action_none)
    private val appActionType by composeTestRule.stringResource(ActionType.APP.nameStringId)
    private val hotkeyActionType by composeTestRule.stringResource(ActionType.HOTKEY.nameStringId)
    private val allActionTypes by lazy { setOf(appActionType, hotkeyActionType) }

    @Test
    fun cancelButton_onClick_dismisses() {
        var dismissed = false
        composeTestRule.setContent {
            CreateActionSelectDialog(onDismiss = { dismissed = true })
        }

        composeTestRule.onNodeWithText(cancel).performClick()

        Assert.assertTrue(dismissed)
    }

    // When all action types are available, all action types are displayed
    @Test
    fun allActionTypesAvailable_allActionTypesAreDisplayed() {
        composeTestRule.setContent {
            CreateActionSelectDialog(availableActionTypes = ActionType.entries.toSet())
        }

        for (actionType in allActionTypes) {
            composeTestRule.onNodeWithText(actionType).assertIsDisplayed()
        }
    }

    // When only one action type is available, other action types don't exist
    @Test
    fun singleActionTypeAvailable_singleAppActionTypeIsDisplayed() {
        composeTestRule.setContent {
            CreateActionSelectDialog(availableActionTypes = setOf(ActionType.APP))
        }

        composeTestRule.onNodeWithText(appActionType).assertIsDisplayed()
        composeTestRule.onNodeWithText(hotkeyActionType).assertDoesNotExist()
    }

    // When initial action is null, `No action` option must stay selected on action type change
    @Test
    fun initialActionIsNull_actionTypeChange_noActionSelected() {
        composeTestRule.setContent {
            CreateActionSelectDialog(
                availableActionTypes = ActionType.entries.toSet(),
                initialAction = null,
            )
        }

        composeTestRule.onNodeWithText(noAction).assertIsSelected()
        for (actionType in allActionTypes) {
            composeTestRule.onNodeWithText(actionType).performClick()
            composeTestRule.onNodeWithText(noAction).assertIsSelected()
        }
    }

    // All actions exist for app action type
    @Test
    fun appActionType_allActions_areDisplayed() {
        composeTestRule.setContent {
            CreateActionSelectDialog(
                availableActionTypes = setOf(ActionType.APP),
            )
        }

        for (appAction in AppAction.entries) {
            val name = composeTestRule.activity.getString(appAction.nameStringId)
            composeTestRule.onNodeWithText(name).assertExists()
        }
    }

    // Given: a long list of Hotkeys that doesn't fit into screen and initially selected
    // hotkey id at the end of a that list.
    // Expected: dialog should scroll to the selected hotkey.
    @Test
    fun initialActionIsHotkey_needsScrolling_isDisplayed() {
        val count = 100
        val hotkeys = buildList {
            repeat(count) {
                val id = it + 1
                val desc = HotkeyDescription.WithString("Desc $id")
                add(HotkeyInfoUIState(id, "Key $id", desc))
            }
        }
        composeTestRule.setContent {
            CreateActionSelectDialog(
                availableActionTypes = ActionType.entries.toSet(),
                initialAction = Action(ActionType.HOTKEY, count),
                hotkeys = hotkeys,
            )
        }

        composeTestRule.onNodeWithText("Key $count").apply {
            assertIsDisplayed()
            assertIsSelected()
        }
    }

    @Test
    fun okButton_onClick_confirmsWithCorrectAction() {
        val count = 5
        val expected = Action(ActionType.HOTKEY, 1)
        var actual: Action? = null
        composeTestRule.setContent {
            val hotkeys = buildList {
                repeat(count) {
                    val id = it + 1
                    val desc = HotkeyDescription.WithString("Desc $id")
                    add(HotkeyInfoUIState(id, "Key $id", desc))
                }
            }
            CreateActionSelectDialog(
                availableActionTypes = ActionType.entries.toSet(),
                initialAction = Action(ActionType.APP, AppAction.DISCONNECT.id),
                hotkeys = hotkeys,
                onConfirm = { actual = it },
            )
        }

        composeTestRule.apply {
            onNodeWithText(hotkeyActionType).performClick()
            onNodeWithText("Key 1").performClick()
            onNodeWithText(ok).performClick()
        }

        Assert.assertEquals(expected, actual)
    }

    @Suppress("TestFunctionName")
    @Composable
    private fun CreateActionSelectDialog(
        availableActionTypes: Set<ActionType> = ActionType.entries.toSet(),
        initialAction: Action? = null,
        hotkeys: List<HotkeyInfoUIState> = emptyList(),
        onConfirm: (Action?) -> Unit = {},
        onDismiss: () -> Unit = {},
    ) {
        SoundRemoteTheme {
            ActionSelectDialog(
                availableActionTypes = availableActionTypes,
                initialAction = initialAction,
                hotkeys = hotkeys,
                onConfirm = onConfirm,
                onDismiss = onDismiss,
            )
        }
    }
}
