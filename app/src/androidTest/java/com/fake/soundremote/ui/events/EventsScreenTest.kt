package com.fake.soundremote.ui.events

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.fake.soundremote.R
import com.fake.soundremote.data.Action
import com.fake.soundremote.data.ActionType
import com.fake.soundremote.data.Event
import com.fake.soundremote.stringResource
import com.fake.soundremote.ui.theme.SoundRemoteTheme
import com.fake.soundremote.util.AppPermission
import com.fake.soundremote.util.TextValue
import org.junit.Rule
import org.junit.Test

internal class EventsScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val navigateUp by composeTestRule.stringResource(R.string.navigate_up)
    private val permissionInfo by composeTestRule.stringResource(R.string.permission_show_info_caption)

    // Events screen should contain navigate up arrow
    @Test
    fun navigateUp_exist() {
        composeTestRule.setContent {
            CreateEventsScreen()
        }

        composeTestRule.onNodeWithContentDescription(navigateUp).assertIsDisplayed()
    }

    // Event name is displayed
    @Test
    fun eventName_isDisplayed() {
        val eventNameStringId = Event.CALL_BEGIN.nameStringId
        composeTestRule.setContent {
            val events = listOf(
                EventUIState(1, eventNameStringId, applicableActionTypes = emptySet()),
            )
            CreateEventsScreen(eventsUIState = EventsUIState(events))
        }

        val eventName by composeTestRule.stringResource(eventNameStringId)
        composeTestRule.onNodeWithText(eventName).assertIsDisplayed()
    }

    // Hotkey name is displayed
    @Test
    fun hotkeyName_isDisplayed() {
        val hotkeyName = "Test name"
        composeTestRule.setContent {
            val events = buildList {
                val action =
                    ActionUIState(ActionType.HOTKEY, 1, TextValue.TextString(hotkeyName))
                add(
                    EventUIState(
                        1,
                        Event.CALL_BEGIN.nameStringId,
                        null,
                        action,
                        emptySet()
                    )
                )
            }
            CreateEventsScreen(eventsUIState = EventsUIState(events))
        }

        composeTestRule.onNodeWithText(hotkeyName, true).assertIsDisplayed()
    }

    // Event without a permission doesn't show permission info button
    @Test
    fun permissionInfoButton_eventWithoutPermission_doesNotExist() {
        composeTestRule.setContent {
            val events = buildList {
                val action =
                    ActionUIState(ActionType.HOTKEY, 1, TextValue.TextString("Hotkey name"))
                add(
                    EventUIState(
                        1,
                        Event.CALL_BEGIN.nameStringId,
                        null,
                        action,
                        emptySet()
                    )
                )
            }
            CreateEventsScreen(eventsUIState = EventsUIState(events))
        }

        composeTestRule.onNodeWithContentDescription(permissionInfo).assertDoesNotExist()
    }

    // Event with a permission shows permission info button
    @Test
    fun permissionInfoButton_eventWithPermission_isDisplayed() {
        composeTestRule.setContent {
            val events = buildList {
                val action =
                    ActionUIState(ActionType.HOTKEY, 1, TextValue.TextString("Hotkey name"))
                add(
                    EventUIState(
                        1,
                        Event.CALL_BEGIN.nameStringId,
                        AppPermission.Phone,
                        action,
                        emptySet()
                    )
                )
            }
            CreateEventsScreen(eventsUIState = EventsUIState(events))
        }

        composeTestRule.onNodeWithContentDescription(permissionInfo).assertIsDisplayed()
    }

    @Suppress("TestFunctionName")
    @Composable
    private fun CreateEventsScreen(
        modifier: Modifier = Modifier,
        eventsUIState: EventsUIState = EventsUIState(),
        onSetActionForEvent: (eventId: Int, action: Action?) -> Unit = { _, _ -> },
        onNavigateUp: () -> Unit = {},
    ) {
        SoundRemoteTheme {
            EventsScreen(
                eventsUIState = eventsUIState,
                onSetActionForEvent = onSetActionForEvent,
                onNavigateUp = onNavigateUp,
                modifier = modifier,
            )
        }
    }
}
