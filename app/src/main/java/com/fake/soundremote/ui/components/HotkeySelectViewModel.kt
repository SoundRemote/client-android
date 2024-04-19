package com.fake.soundremote.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fake.soundremote.data.HotkeyRepository
import com.fake.soundremote.util.HotkeyDescription
import com.fake.soundremote.util.generateDescription
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HotkeyInfoUIState(
    val id: Int,
    val name: String,
    val description: HotkeyDescription,
)

@HiltViewModel
class HotkeySelectViewModel @Inject constructor(
    hotkeyRepository: HotkeyRepository,
) : ViewModel() {
    val hotkeysState = hotkeyRepository.getAllInfoOrdered()
        .map { hotkeys ->
            hotkeys.map { hotkey ->
                HotkeyInfoUIState(
                    hotkey.id,
                    hotkey.name,
                    description = generateDescription(hotkey),
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}
