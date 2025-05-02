package io.github.soundremote.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import io.github.soundremote.R
import io.github.soundremote.data.Action
import io.github.soundremote.data.ActionType
import io.github.soundremote.data.AppAction
import io.github.soundremote.stringResource
import io.github.soundremote.ui.theme.SoundRemoteTheme
import io.github.soundremote.util.HotkeyDescription
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
    fun appActionType_allActions_exist() {
        composeTestRule.setContent {
            CreateActionSelectDialog(
                availableActionTypes = setOf(ActionType.APP),
            )
        }

        for (appAction in AppAction.entries) {
            val name = composeTestRule.activity.getString(appAction.nameStringId)
            composeTestRule.apply {
                onNode(hasScrollAction()).performScrollToNode(hasText(name))
                onNodeWithText(name).assertExists()
            }
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
        val hotkeys = buildList {
            repeat(5) {
                val id = it + 1
                val desc = HotkeyDescription.WithString("Desc $id")
                add(HotkeyInfoUIState(id, "Key $id", desc))
            }
        }
        val expected = Action(ActionType.HOTKEY, 1)
        var actual: Action? = null
        composeTestRule.setContent {
            CreateActionSelectDialog(
                availableActionTypes = ActionType.entries.toSet(),
                initialAction = Action(ActionType.APP, AppAction.DISCONNECT.id),
                hotkeys = hotkeys,
                onConfirm = { actual = it },
            )
        }

        composeTestRule.apply {
            // Select hotkey actions
            onNodeWithText(hotkeyActionType).performClick()

            // Hotkeys are in a lazy list, we need to scroll it first
            onNode(hasScrollAction()).performScrollToNode(hasText("Key 1"))
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
