package com.fake.soundremote.ui

import com.fake.soundremote.MainDispatcherExtension
import com.fake.soundremote.data.Event
import com.fake.soundremote.data.EventAction
import com.fake.soundremote.data.Keystroke
import com.fake.soundremote.data.TestEventActionRepository
import com.fake.soundremote.data.TestKeystrokeRepository
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
    private var keystrokeRepository = TestKeystrokeRepository()
    private var eventActionRepository = TestEventActionRepository()
    private lateinit var viewModel: EventsViewModel

    @BeforeEach
    fun setup() {
        viewModel = EventsViewModel(eventActionRepository, keystrokeRepository)
    }

    @DisplayName("setKeystrokeForEvent")
    @Nested
    inner class SetKeystrokeForEventTests {
        @Test
        @DisplayName("sets action for an event without action")
        fun eventWithoutAction_existingAction_setsAction() = runTest {
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }

            val keystrokeId = 10
            val keystrokes = listOf(Keystroke(keystrokeId, 100, 0, "TestK", false, 0))
            keystrokeRepository.setKeystrokes(keystrokes)
            val eventId = Event.CALL_END.id
            assertNull(viewModel.uiState.value.events.find { it.id == eventId }?.keystrokeId)

            viewModel.setKeystrokeForEvent(eventId, keystrokeId)

            val actual = viewModel.uiState.value.events.find { it.id == eventId }?.keystrokeId
            assertEquals(keystrokeId, actual)

            collectJob.cancel()
        }

        @Test
        @DisplayName("removes action from an event with action")
        fun eventWithAction_nullAction_removesAction() = runTest {
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }

            val keystrokeId = 1
            val keystrokes = listOf(Keystroke(keystrokeId, 100, 0, "TestK", false, 0))
            keystrokeRepository.setKeystrokes(keystrokes)
            val eventId = Event.CALL_BEGIN.id
            val eventActions = listOf(EventAction(eventId, keystrokeId))
            eventActionRepository.setEventActions(eventActions)
            assertTrue(viewModel.uiState.value.events.find { it.id == eventId }?.keystrokeId == keystrokeId)

            viewModel.setKeystrokeForEvent(eventId, null)

            val actual = viewModel.uiState.value.events.find { it.id == eventId }?.keystrokeId
            assertNull(actual)

            collectJob.cancel()
        }

        @Test
        @DisplayName("updates action of an event with other action")
        fun eventWithAction_existingAction_updatesAction() = runTest {
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }

            val oldKeystrokeId = 1
            val newKeystrokeId = 2
            val keystrokes = listOf(
                Keystroke(oldKeystrokeId, 100, 0, "TestK", false, 0),
                Keystroke(newKeystrokeId, 100, 0, "TestK", false, 0),
            )
            keystrokeRepository.setKeystrokes(keystrokes)
            val eventId = Event.CALL_BEGIN.id
            val eventActions = listOf(EventAction(eventId, oldKeystrokeId))
            eventActionRepository.setEventActions(eventActions)
            assertEquals(
                oldKeystrokeId,
                viewModel.uiState.value.events.find { it.id == eventId }?.keystrokeId
            )

            viewModel.setKeystrokeForEvent(eventId, newKeystrokeId)

            val actual = viewModel.uiState.value.events.find { it.id == eventId }?.keystrokeId
            assertEquals(newKeystrokeId, actual)

            collectJob.cancel()
        }
    }
}