package com.fake.soundremote.data

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith

@RunWith(Enclosed::class)
internal class AppDatabaseTest {
    @Ignore
    companion object {

        private val dispatcher = StandardTestDispatcher()

        @JvmStatic
        @BeforeClass
        fun setUpClass() {
        }
    }

    internal class TriggersAndForeignKeysTests {
        @JvmField
        @Rule
        val database = DatabaseResource(dispatcher)

        @Test
        fun createKeystroke_setsOrderToDefaultValue() = runTest(dispatcher) {
            val expected = Keystroke.ORDER_DEFAULT_VALUE
            val keystrokeId = database.keystrokeRepository.insert(Keystroke(1, "Test"))
            val actual = database.keystrokeRepository.getById(keystrokeId.toInt())?.order

            assertThat(
                "Creating a Keystroke must init the order field with the default value",
                actual, equalTo(expected)
            )
        }

        @Test
        fun deleteEventBoundKeystroke_unbindsEvent() = runTest(dispatcher) {
            val eventId = Event.CALL_BEGIN.id
            val keystrokeId = database.keystrokeRepository
                .insert(Keystroke(1, "Test")).toInt()
            database.eventActionRepository.insert(EventAction(eventId, keystrokeId))

            database.keystrokeRepository.deleteById(keystrokeId)

            val actual: EventAction? = database.eventActionRepository.getById(eventId)
            assertThat(
                "Deleting Event bound Keystroke must delete the Event",
                actual, nullValue()
            )
        }
    }
}
