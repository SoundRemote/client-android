package io.github.soundremote.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.soundremote.util.DEFAULT_AUDIO_COMPRESSION
import io.github.soundremote.util.DEFAULT_CLIENT_PORT
import io.github.soundremote.util.DEFAULT_IGNORE_AUDIO_FOCUS
import io.github.soundremote.util.DEFAULT_SERVER_ADDRESS
import io.github.soundremote.util.DEFAULT_SERVER_PORT
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class SettingsScreenPreferences(
    val serverPort: Int,
    val clientPort: Int,
    val audioCompression: Int,
    val ignoreAudioFocus: Boolean,
)

private const val KEY_SERVER_PORT = "server_port"
private const val KEY_CLIENT_PORT = "client_port"
private const val KEY_SERVER_ADDRESSES = "server_addresses"
private const val KEY_AUDIO_COMPRESSION = "audio_compression"
private const val KEY_IGNORE_AUDIO_FOCUS = "ignore_audio_focus"

private const val SERVER_ADDRESSES_DELIMITER = ';'
private const val SERVER_ADDRESSES_LIMIT = 5

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : PreferencesRepository {

    private object PreferencesKeys {
        val SERVER_ADDRESSES = stringPreferencesKey(KEY_SERVER_ADDRESSES)
        val SERVER_PORT = intPreferencesKey(KEY_SERVER_PORT)
        val CLIENT_PORT = intPreferencesKey(KEY_CLIENT_PORT)
        val AUDIO_COMPRESSION = intPreferencesKey(KEY_AUDIO_COMPRESSION)
        val IGNORE_AUDIO_FOCUS = booleanPreferencesKey(KEY_IGNORE_AUDIO_FOCUS)
    }

    private val preferencesFlow = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    override val settingsScreenPreferencesFlow: Flow<SettingsScreenPreferences> = preferencesFlow
        .map { preferences ->
            SettingsScreenPreferences(
                preferences[PreferencesKeys.SERVER_PORT] ?: DEFAULT_SERVER_PORT,
                preferences[PreferencesKeys.CLIENT_PORT] ?: DEFAULT_CLIENT_PORT,
                preferences[PreferencesKeys.AUDIO_COMPRESSION] ?: DEFAULT_AUDIO_COMPRESSION,
                preferences[PreferencesKeys.IGNORE_AUDIO_FOCUS] ?: DEFAULT_IGNORE_AUDIO_FOCUS,
            )
        }

    override val serverAddressesFlow: Flow<List<String>> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.SERVER_ADDRESSES]
                ?.split(SERVER_ADDRESSES_DELIMITER) ?: listOf(DEFAULT_SERVER_ADDRESS)
        }

    override val audioCompressionFlow: Flow<Int> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.AUDIO_COMPRESSION] ?: DEFAULT_AUDIO_COMPRESSION
        }.distinctUntilChanged()

    override val ignoreAudioFocusFlow: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.IGNORE_AUDIO_FOCUS] ?: DEFAULT_IGNORE_AUDIO_FOCUS
        }.distinctUntilChanged()

    override suspend fun setServerAddress(serverAddress: String) {
        val current = LinkedHashSet(serverAddressesFlow.first())
        current.remove(serverAddress)
        current.add(serverAddress)
        while (current.size > SERVER_ADDRESSES_LIMIT) {
            current.remove(current.first())
        }
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.SERVER_ADDRESSES] = current
                .joinToString(SERVER_ADDRESSES_DELIMITER.toString())
        }
    }

    override suspend fun getServerAddress(): String = preferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.SERVER_ADDRESSES]
                ?.substringAfterLast(SERVER_ADDRESSES_DELIMITER) ?: DEFAULT_SERVER_ADDRESS
        }.first()

    override suspend fun setServerPort(value: Int) {
        dataStore.edit { prefs ->
            prefs[intPreferencesKey(KEY_SERVER_PORT)] = value
        }
    }

    override suspend fun getServerPort(): Int =
        settingsScreenPreferencesFlow.first().serverPort

    override suspend fun setClientPort(value: Int) {
        dataStore.edit { prefs ->
            prefs[intPreferencesKey(KEY_CLIENT_PORT)] = value
        }
    }

    override suspend fun getClientPort(): Int =
        settingsScreenPreferencesFlow.first().clientPort

    override suspend fun setAudioCompression(value: Int) {
        dataStore.edit { prefs ->
            prefs[intPreferencesKey(KEY_AUDIO_COMPRESSION)] = value
        }
    }

    override suspend fun getAudioCompression(): Int =
        audioCompressionFlow.first()

    override suspend fun setIgnoreAudioFocus(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[booleanPreferencesKey(KEY_IGNORE_AUDIO_FOCUS)] = value
        }
    }

    override suspend fun getIgnoreAudioFocus(): Boolean =
        settingsScreenPreferencesFlow.first().ignoreAudioFocus
}
