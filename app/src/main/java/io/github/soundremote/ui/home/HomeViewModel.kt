package io.github.soundremote.ui.home

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.common.net.InetAddresses
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.soundremote.R
import io.github.soundremote.data.HotkeyRepository
import io.github.soundremote.data.preferences.PreferencesRepository
import io.github.soundremote.service.ServiceRepository
import io.github.soundremote.util.ConnectionStatus
import io.github.soundremote.util.HotkeyDescription
import io.github.soundremote.util.Key
import io.github.soundremote.util.generateDescription
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUIState(
    val hotkeys: List<HomeHotkeyUIState> = emptyList(),
    val serverAddress: String = "",
    val recentServersAddresses: List<String> = emptyList(),
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val isMuted: Boolean = false,
)

data class HomeHotkeyUIState(
    val id: Int,
    val name: String,
    val description: HotkeyDescription,
)

@HiltViewModel
internal class HomeViewModel @Inject constructor(
    private val userPreferencesRepo: PreferencesRepository,
    private val hotkeyRepository: HotkeyRepository,
    private val serviceRepository: ServiceRepository,
) : ViewModel() {

    val homeUIState: StateFlow<HomeUIState> = combine(
        hotkeyRepository.getFavouredOrdered(true),
        userPreferencesRepo.serverAddressesFlow,
        serviceRepository.serviceState,
    ) { hotkeys, addresses, serviceState ->
        val hotkeyStates = hotkeys.map { hotkey ->
            HomeHotkeyUIState(
                id = hotkey.id,
                name = hotkey.name,
                description = generateDescription(
                    keyCode = hotkey.keyCode,
                    mods = hotkey.mods
                ),
            )
        }
        HomeUIState(
            hotkeys = hotkeyStates,
            serverAddress = addresses.last(),
            recentServersAddresses = addresses,
            connectionStatus = serviceState.connectionStatus,
            isMuted = serviceState.isMuted,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUIState()
    )
    var messageState by mutableStateOf<Int?>(null)
        private set

    private fun setServerAddress(address: String) {
        viewModelScope.launch {
            userPreferencesRepo.setServerAddress(address)
        }
    }

    private fun setMessage(@StringRes messageId: Int) {
        messageState = messageId
    }

    fun messageShown() {
        messageState = null
    }

    fun connect(address: String) {
        val newAddress = address.trim()
        if (InetAddresses.isInetAddress(newAddress)) {
            setServerAddress(newAddress)
            serviceRepository.connect(newAddress)
        } else {
            setMessage(R.string.message_invalid_address)
        }
    }

    fun disconnect() {
        serviceRepository.disconnect()
    }

    fun sendHotkey(hotkeyId: Int) {
        viewModelScope.launch {
            hotkeyRepository.getById(hotkeyId)?.let {
                serviceRepository.sendHotkey(it)
            }
        }
    }

    fun sendKey(key: Key) {
        serviceRepository.sendKey(key)
    }

    fun setMuted(value: Boolean) {
        serviceRepository.setMuted(value)
    }
}
