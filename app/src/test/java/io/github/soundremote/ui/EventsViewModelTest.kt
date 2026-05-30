package io.github.soundremote.ui

import io.github.soundremote.MainDispatcherExtension
import io.github.soundremote.data.Action
import io.github.soundremote.data.ActionData
import io.github.soundremote.data.ActionType
import io.github.soundremote.data.Event
import io.github.soundremote.data.EventAction
import io.github.soundremote.data.TestEventActionRepository
import io.github.soundremote.data.TestHotkeyRepository
import io.github.soundremote.getHotkey
import io.github.soundremote.ui.events.EventsViewModel
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
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
            viewModel.uiState.value.events.find { it.id == eventId }?.action.shouldBeNull()

            viewModel.setActionForEvent(eventId, Action(ActionType.HOTKEY, expectedId))

            val actual = viewModel.uiState.value.events.find { it.id == eventId }?.action?.id
            actual shouldBe expectedId

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
            viewModel.uiState.value.events.find { it.id == eventId }?.action?.id shouldBe expectedId

            viewModel.setActionForEvent(eventId, null)

            val actual = viewModel.uiState.value.events.find { it.id == eventId }?.action
            actual.shouldBeNull()

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
            viewModel.uiState.value.events.find { it.id == eventId }?.action?.id shouldBe oldHotkeyId

            viewModel.setActionForEvent(eventId, Action(ActionType.HOTKEY, newHotkeyId))

            val actual = viewModel.uiState.value.events.find { it.id == eventId }?.action?.id
            actual shouldBe newHotkeyId

            collectJob.cancel()
        }
    }
}
