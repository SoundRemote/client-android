package com.fake.soundremote.ui

import com.fake.soundremote.MainDispatcherExtension
import com.fake.soundremote.data.Hotkey
import com.fake.soundremote.data.TestHotkeyRepository
import com.fake.soundremote.getHotkey
import com.fake.soundremote.ui.hotkeylist.HotkeyListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainDispatcherExtension::class)
@DisplayName("HotkeyListViewModel")
class HotkeyListViewModelTest {
    private var hotkeyRepository = TestHotkeyRepository()

    private lateinit var viewModel: HotkeyListViewModel

    @BeforeEach
    fun setup() {
        viewModel = HotkeyListViewModel(hotkeyRepository)
    }

    @Test
    @DisplayName("deleteHotkey() deletes")
    fun deleteHotkey_deletes() = runTest {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.hotkeyListState.collect {}
        }

        val id = 10
        val hotkeys = listOf(
            getHotkey(id = id),
            getHotkey(id = id + 1),
        )
        hotkeyRepository.setHotkeys(hotkeys)

        viewModel.deleteHotkey(id)

        val actual = viewModel.hotkeyListState.value.hotkeys.find { it.id == id }
        assertNull(actual)

        collectJob.cancel()
    }

    @Test
    @DisplayName("changeFavoured() changes favoured status")
    fun changeFavoured_changes() = runTest {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.hotkeyListState.collect {}
        }

        val id = 10
        val hotkeys = listOf(getHotkey(id = id, favoured = true))
        hotkeyRepository.setHotkeys(hotkeys)

        viewModel.changeFavoured(id, false)

        val actual = viewModel.hotkeyListState.value.hotkeys.find { it.id == id }!!.favoured
        assertFalse(actual)

        collectJob.cancel()
    }

    @ParameterizedTest(name = "from {1} to {2} results in {3}")
    @MethodSource("com.fake.soundremote.ui.HotkeyListViewModelTest#moveHotkeyProvider")
    @DisplayName("moveHotkey() moves correctly")
    fun moveHotkey_movesCorrectly(
        hotkeys: List<Hotkey>,
        from: Int,
        to: Int,
        expected: List<Int>
    ) = runTest {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.hotkeyListState.collect {}
        }

        hotkeyRepository.setHotkeys(hotkeys)

        viewModel.moveHotkey(from, to)

        val actual = viewModel.hotkeyListState.value.hotkeys.map { it.id }
        assertIterableEquals(expected, actual)

        collectJob.cancel()
    }

    companion object {
        /**
         * List<[Hotkey]>, from: Int, to: Int, expectedOrderedIds: List`<Int>`
         */
        @JvmStatic
        fun moveHotkeyProvider(): Stream<Arguments> = Stream.of(
            Arguments.arguments(
                generateZeroOrderHotkeys(10),
                3,
                8,
                listOf(1, 2, 3, 5, 6, 7, 8, 9, 4, 10),
            ),
            Arguments.arguments(
                generateZeroOrderHotkeys(10),
                9,
                0,
                listOf(10, 1, 2, 3, 4, 5, 6, 7, 8, 9)
            ),
            Arguments.arguments(
                generateZeroOrderHotkeys(8),
                5,
                5,
                listOf(1, 2, 3, 4, 5, 6, 7, 8)
            ),
        )

        /**
         * Generates hotkeys with order value of 0
         */
        private fun generateZeroOrderHotkeys(n: Int): List<Hotkey> = buildList {
            repeat(n) {
                val id = it + 1
                add(getHotkey(id = id, order = 0))
            }
        }
    }
}
