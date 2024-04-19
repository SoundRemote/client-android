package com.fake.soundremote.ui

import com.fake.soundremote.MainDispatcherExtension
import com.fake.soundremote.data.Action
import com.fake.soundremote.data.ActionData
import com.fake.soundremote.data.ActionType
import com.fake.soundremote.data.Event
import com.fake.soundremote.data.EventAction
import com.fake.soundremote.data.TestEventActionRepository
import com.fake.soundremote.data.TestHotkeyRepository
import com.fake.soundremote.getHotkey
import com.fake.soundremote.ui.events.EventsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainDispatcherExtension::class)
@DisplayName("EventsViewModel")
internal class EventsViewModelTest {
    private var hotkeyRepository = TestHotkeyRepository()
    private var eventActionRepository = TestEventActionRepository()
    private lateinit var viewModel: EventsViewModel

    @BeforeEach
    fun setup() {
        viewModel = EventsViewModel(eventActionRepository, hotkeyRepository)
    }

    @DisplayName("setHotkeyForEvent")
    @Nested
    inner class SetHotkeyForEventTests {
        @Test
        @DisplayName("sets action for an event without action")
        fun eventWithoutAction_existingAction_setsAction() = runTest {
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }

            val expectedId = 10
            val hotkeys = listOf(getHotkey(id = expectedId))
            hotkeyRepository.setHotkeys(hotkeys)
            val eventId = Event.CALL_END.id
            assertNull(viewModel.uiState.value.events.find { it.id == eventId }?.action)

            viewModel.setActionForEvent(eventId, Action(ActionType.HOTKEY, expectedId))

            val actual = viewModel.uiState.value.events.find { it.id == eventId }?.action?.id
            assertEquals(expectedId, actual)

            collectJob.cancel()
        }

        @Test
        @DisplayName("removes action from an event with action")
        fun eventWithAction_nullAction_removesAction() = runTest {
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }

            val expectedId = 1
            val hotkeys = listOf(getHotkey(id = expectedId))
            hotkeyRepository.setHotkeys(hotkeys)
            val eventId = Event.CALL_BEGIN.id
            val eventActions =
                listOf(EventAction(eventId, ActionData(ActionType.HOTKEY, expectedId)))
            eventActionRepository.setEventActions(eventActions)
            assertTrue(viewModel.uiState.value.events.find { it.id == eventId }?.action?.id == expectedId)

            viewModel.setActionForEvent(eventId, null)

            val actual = viewModel.uiState.value.events.find { it.id == eventId }?.action
            assertNull(actual)

            collectJob.cancel()
        }

        @Test
        @DisplayName("updates action of an event with another action")
        fun eventWithAction_existingAction_updatesAction() = runTest {
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }

            val oldHotkeyId = 1
            val newHotkeyId = 2
            val hotkeys = listOf(
                getHotkey(id = oldHotkeyId),
                getHotkey(id = newHotkeyId),
            )
            hotkeyRepository.setHotkeys(hotkeys)
            val eventId = Event.CALL_BEGIN.id
            val eventActions =
                listOf(EventAction(eventId, ActionData(ActionType.HOTKEY, oldHotkeyId)))
            eventActionRepository.setEventActions(eventActions)
            assertEquals(
                oldHotkeyId,
                viewModel.uiState.value.events.find { it.id == eventId }?.action?.id
            )

            viewModel.setActionForEvent(eventId, Action(ActionType.HOTKEY, newHotkeyId))

            val actual = viewModel.uiState.value.events.find { it.id == eventId }?.action?.id
            assertEquals(newHotkeyId, actual)

            collectJob.cancel()
        }
    }
}
