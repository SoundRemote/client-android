package io.github.soundremote.data.preferences

import io.github.soundremote.util.DEFAULT_AUDIO_COMPRESSION
import io.github.soundremote.util.DEFAULT_CLIENT_PORT
import io.github.soundremote.util.DEFAULT_IGNORE_AUDIO_FOCUS
import io.github.soundremote.util.DEFAULT_SERVER_ADDRESS
import io.github.soundremote.util.DEFAULT_SERVER_PORT
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

class TestPreferencesRepository : PreferencesRepository {
    private val _settingsScreenPreferencesFlow = MutableStateFlow(
        SettingsScreenPreferences(
            DEFAULT_SERVER_PORT,
            DEFAULT_CLIENT_PORT,
            DEFAULT_AUDIO_COMPRESSION,
            DEFAULT_IGNORE_AUDIO_FOCUS,
        )
    )
    override val settingsScreenPreferencesFlow: Flow<SettingsScreenPreferences>
        get() = _settingsScreenPreferencesFlow

    private val serverAddressesLimit = 5
    private val _serverAddressesFlow = MutableStateFlow(listOf(DEFAULT_SERVER_ADDRESS))
    override val serverAddressesFlow: Flow<List<String>>
        get() = _serverAddressesFlow

    private val _audioCompressionFlow = MutableStateFlow(DEFAULT_AUDIO_COMPRESSION)
    override val audioCompressionFlow: Flow<Int>
        get() = _audioCompressionFlow

    private val _ignoreAudioFocusFlow = MutableStateFlow(DEFAULT_IGNORE_AUDIO_FOCUS)
    override val ignoreAudioFocusFlow: Flow<Boolean>
        get() = _ignoreAudioFocusFlow

    override suspend fun setServerAddress(serverAddress: String) {
        val current = LinkedHashSet(serverAddressesFlow.first())
        current.remove(serverAddress)
        current.add(serverAddress)
        while (current.size > serverAddressesLimit) {
            current.remove(current.first())
        }
        _serverAddressesFlow.value = current.toList()
    }

    override suspend fun getServerAddress(): String {
        return _serverAddressesFlow.value.last()
    }

    override suspend fun setServerPort(value: Int) {
        _settingsScreenPreferencesFlow.value =
            _settingsScreenPreferencesFlow.value.copy(serverPort = value)
    }

    override suspend fun getServerPort(): Int {
        return _settingsScreenPreferencesFlow.value.serverPort
    }

    override suspend fun setClientPort(value: Int) {
        _settingsScreenPreferencesFlow.value =
            _settingsScreenPreferencesFlow.value.copy(clientPort = value)
    }

    override suspend fun getClientPort(): Int {
        return _settingsScreenPreferencesFlow.value.clientPort
    }

    override suspend fun setAudioCompression(value: Int) {
        _settingsScreenPreferencesFlow.value =
            _settingsScreenPreferencesFlow.value.copy(audioCompression = value)
        _audioCompressionFlow.value = value
    }

    override suspend fun getAudioCompression(): Int {
        return _audioCompressionFlow.value
    }

    override suspend fun setIgnoreAudioFocus(value: Boolean) {
        _settingsScreenPreferencesFlow.value =
            _settingsScreenPreferencesFlow.value.copy(ignoreAudioFocus = value)
        _ignoreAudioFocusFlow.value = value
    }

    override suspend fun getIgnoreAudioFocus(): Boolean {
        return _settingsScreenPreferencesFlow.value.ignoreAudioFocus
    }
}
