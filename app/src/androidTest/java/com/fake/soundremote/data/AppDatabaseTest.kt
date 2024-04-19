package com.fake.soundremote.data

import com.fake.soundremote.util.KeyCode
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
        fun createHotkey_setsOrderToDefaultValue() = runTest(dispatcher) {
            val expected = Hotkey.ORDER_DEFAULT_VALUE
            val hotkeyId = database.hotkeyRepository.insert(Hotkey(KeyCode(1), "Test"))

            val actual = database.hotkeyRepository.getById(hotkeyId.toInt())?.order

            assertThat(
                "Creating a Hotkey must init the order field with the default value",
                actual, equalTo(expected)
            )
        }

        @Test
        fun deleteEventBoundHotkey_deletesEventAction() = runTest(dispatcher) {
            val hotkey = Hotkey(KeyCode(123), "Test")
            val hotkeyId = database.hotkeyRepository.insert(hotkey).toInt()
            val eventId = Event.CALL_END.id
            val eventAction = EventAction(eventId, ActionData(ActionType.HOTKEY, hotkeyId))
            database.eventActionRepository.insert(eventAction)

            database.hotkeyRepository.deleteById(hotkeyId)

            val actual: EventAction? = database.eventActionRepository.getById(eventId)
            assertThat(
                "Deleting Event bound Hotkey must delete the EventAction",
                actual, nullValue()
            )
        }
    }
}
