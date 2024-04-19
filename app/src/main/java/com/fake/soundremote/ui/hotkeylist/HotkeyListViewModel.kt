package com.fake.soundremote.ui.hotkeylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fake.soundremote.data.HotkeyOrder
import com.fake.soundremote.data.HotkeyRepository
import com.fake.soundremote.util.HotkeyDescription
import com.fake.soundremote.util.generateDescription
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HotkeyUIState(
    val id: Int,
    val name: String,
    val description: HotkeyDescription,
    val favoured: Boolean
)

data class HotkeyListUIState(
    val hotkeys: List<HotkeyUIState> = emptyList()
)

@HiltViewModel
class HotkeyListViewModel @Inject constructor(
    private val hotkeyRepository: HotkeyRepository,
) : ViewModel() {
    val hotkeyListState: StateFlow<HotkeyListUIState> = hotkeyRepository.getAllOrdered()
        .map { hotkeys ->
            val hotkeyUIStates = hotkeys.map { hotkey ->
                HotkeyUIState(
                    hotkey.id,
                    hotkey.name,
                    description = generateDescription(hotkey),
                    favoured = hotkey.isFavoured
                )
            }
            HotkeyListUIState(hotkeyUIStates)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HotkeyListUIState()
        )

    fun moveHotkey(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val orderedIds = hotkeyListState.value.hotkeys.map { it.id }.toMutableList()
            require(fromIndex in orderedIds.indices && toIndex in orderedIds.indices) { "Invalid indices" }
            orderedIds.add(toIndex, orderedIds.removeAt(fromIndex))
            val orders =
                orderedIds.mapIndexed { index, id -> HotkeyOrder(id, orderedIds.size - index) }
            hotkeyRepository.updateOrders(orders)
        }
    }

    fun deleteHotkey(id: Int) {
        viewModelScope.launch {
            hotkeyRepository.deleteById(id)
        }
    }

    fun changeFavoured(hotkeyId: Int, favoured: Boolean) {
        viewModelScope.launch {
            hotkeyRepository.changeFavoured(hotkeyId, favoured)
        }
    }
}
