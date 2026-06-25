package io.github.soundremote.service

import io.github.soundremote.data.Hotkey
import io.github.soundremote.util.ConnectionStatus
import io.github.soundremote.util.Key
import io.github.soundremote.util.SystemMessage
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

internal class TestServiceManager : ServiceManager {

    private val _serviceState = MutableStateFlow(ServiceState())
    override val serviceState: StateFlow<ServiceState>
        get() = _serviceState
    override val systemMessages: ReceiveChannel<SystemMessage>
        get() = TODO("Not yet implemented")

    override fun bind() {
        TODO("Not yet implemented")
    }

    override fun unbind() {
        TODO("Not yet implemented")
    }

    override fun connect(address: String) {
        _serviceState.update {
            it.copy(connectionStatus = ConnectionStatus.CONNECTED)
        }
    }

    override fun disconnect() {
        _serviceState.update {
            it.copy(connectionStatus = ConnectionStatus.DISCONNECTED)
        }
    }

    override fun sendHotkey(hotkey: Hotkey) {
        sentHotkey = hotkey
    }

    override fun sendKey(key: Key) {
        sentKey = key
    }

    override fun setMuted(value: Boolean) {
        _serviceState.update {
            it.copy(isMuted = value)
        }
    }

    // Test only
    fun setServiceState(state: ServiceState) {
        _serviceState.value = state
    }

    var sentHotkey: Hotkey? = null
    var sentKey: Key? = null
}
