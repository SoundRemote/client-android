package io.github.soundremote.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.soundremote.data.preferences.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    val settings: StateFlow<SettingsUIState> =
        preferencesRepository.settingsScreenPreferencesFlow.map { prefs ->
            SettingsUIState(
                serverPort = prefs.serverPort,
                clientPort = prefs.clientPort,
                audioCompression = prefs.audioCompression,
                ignoreAudioFocus = prefs.ignoreAudioFocus,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUIState(),
        )

    fun setServerPort(value: Int) {
        viewModelScope.launch { preferencesRepository.setServerPort(value) }
    }

    fun setClientPort(value: Int) {
        viewModelScope.launch { preferencesRepository.setClientPort(value) }
    }

    fun setAudioCompression(value: Int) {
        viewModelScope.launch { preferencesRepository.setAudioCompression(value) }
    }

    fun setIgnoreAudioFocus(value: Boolean) {
        viewModelScope.launch { preferencesRepository.setIgnoreAudioFocus(value) }
    }
}

data class SettingsUIState(
    val serverPort: Int = 0,
    val clientPort: Int = 0,
    val audioCompression: Int = 0,
    val ignoreAudioFocus: Boolean = false,
)
