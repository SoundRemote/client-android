package io.github.soundremote.data.preferences

import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {

    val settingsScreenPreferencesFlow: Flow<SettingsScreenPreferences>

    /**
     * Recent server addresses, from the oldest to the most recent
     */
    val serverAddressesFlow: Flow<List<String>>

    val audioCompressionFlow: Flow<Int>

    val ignoreAudioFocusFlow: Flow<Boolean>

    suspend fun setServerAddress(serverAddress: String)

    suspend fun getServerAddress(): String

    suspend fun setServerPort(value: Int)

    suspend fun getServerPort(): Int

    suspend fun setClientPort(value: Int)

    suspend fun getClientPort(): Int

    suspend fun setAudioCompression(value: Int)

    suspend fun getAudioCompression(): Int

    suspend fun setIgnoreAudioFocus(value: Boolean)

    suspend fun getIgnoreAudioFocus(): Boolean
}
