package io.github.soundremote.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.soundremote.util.DEFAULT_AUDIO_COMPRESSION
import io.github.soundremote.util.DEFAULT_CLIENT_PORT
import io.github.soundremote.util.DEFAULT_IGNORE_AUDIO_FOCUS
import io.github.soundremote.util.DEFAULT_SERVER_ADDRESS
import io.github.soundremote.util.DEFAULT_SERVER_PORT
import io.github.soundremote.util.Net
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Path

@DisplayName("UserPreferencesRepository")
@OptIn(ExperimentalCoroutinesApi::class)
class UserPreferencesRepositoryTest {

    private val dataStoreScope = TestScope(UnconfinedTestDispatcher())

    @TempDir
    lateinit var tempDir: Path

    private lateinit var subject: UserPreferencesRepository

    @BeforeEach
    fun setup() {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
        ) {
            tempDir.resolve("test_user_preferences.preferences_pb").toFile()
        }
        subject = UserPreferencesRepository(dataStore)
    }

    @Test
    @DisplayName("default client port value is correct")
    fun getClientPort_defaultValue() = runTest {
        val actual = subject.getClientPort()

        assertEquals(DEFAULT_CLIENT_PORT, actual)
    }

    @Test
    @DisplayName("default server port value is correct")
    fun getServerPort_defaultValue() = runTest {
        val actual = subject.getServerPort()

        assertEquals(DEFAULT_SERVER_PORT, actual)
    }

    @Test
    @DisplayName("default server address value is correct")
    fun getServerAddress_defaultValue() = runTest {
        val actual = subject.getServerAddress()

        assertEquals(DEFAULT_SERVER_ADDRESS, actual)
    }

    @Test
    @DisplayName("default audio compression value is correct")
    fun getAudioCompression_defaultValue() = runTest {
        val actual = subject.getAudioCompression()

        assertEquals(DEFAULT_AUDIO_COMPRESSION, actual)
    }

    @Test
    @DisplayName("setClientPort sets client port correctly")
    fun setClientPort_setsCorrectly() = runTest {
        val expected = DEFAULT_CLIENT_PORT + 123

        subject.setClientPort(expected)
        val actual = subject.getClientPort()

        assertEquals(expected, actual)
    }

    @Test
    @DisplayName("setServerPort sets server port correctly")
    fun setServerPort_setsCorrectly() = runTest {
        val expected = DEFAULT_SERVER_PORT + 123

        subject.setServerPort(expected)
        val actual = subject.getServerPort()

        assertEquals(expected, actual)
    }

    @Test
    @DisplayName("setServerAddress sets server address correctly")
    fun setServerAddress_setsCorrectly() = runTest {
        val expected = "123.45.67.89"

        subject.setServerAddress(expected)
        val actual = subject.getServerAddress()

        assertEquals(expected, actual)
    }

    @Test
    @DisplayName("setAudioCompression sets audio compression correctly")
    fun setAudioCompression_setsCorrectly() = runTest {
        val expected = Net.COMPRESSION_320
        assertNotEquals(DEFAULT_AUDIO_COMPRESSION, expected)

        subject.setAudioCompression(expected)
        val actual = subject.getAudioCompression()

        assertEquals(expected, actual)
    }

    @Test
    @DisplayName("settingsScreenPreferencesFlow updates client port")
    fun settingsScreenPreferencesFlow_updatesClientPort() = runTest {
        val expected = DEFAULT_CLIENT_PORT + 100

        subject.setClientPort(expected)
        val actual = subject.settingsScreenPreferencesFlow.first().clientPort

        assertEquals(expected, actual)
    }

    @Test
    @DisplayName("settingsScreenPreferencesFlow updates server port")
    fun settingsScreenPreferencesFlow_updatesServerPort() = runTest {
        val expected = DEFAULT_SERVER_PORT + 100

        subject.setServerPort(expected)
        val actual = subject.settingsScreenPreferencesFlow.first().serverPort

        assertEquals(expected, actual)
    }

    @Test
    @DisplayName("settingsScreenPreferencesFlow updates audio compression")
    fun settingsScreenPreferencesFlow_updatesAudioCompression() = runTest {
        val expected = Net.COMPRESSION_320
        assertNotEquals(DEFAULT_AUDIO_COMPRESSION, expected)

        subject.setAudioCompression(expected)
        val actual = subject.settingsScreenPreferencesFlow.first().audioCompression

        assertEquals(expected, actual)
    }

    @Test
    @DisplayName("serverAddressesFlow updates")
    fun serverAddressFlow_updates() = runTest {
        val expected = "123.45.67.89"
        val initialAddresses = subject.serverAddressesFlow.first()
        // Make sure that initial list contains only the default address
        assertEquals(listOf(DEFAULT_SERVER_ADDRESS), initialAddresses)

        subject.setServerAddress(expected)
        val actual = subject.serverAddressesFlow.first().last()

        assertEquals(expected, actual)
    }

    @Test
    @DisplayName("audioCompressionFlow updates")
    fun audioCompressionFlow_updates() = runTest {
        val expected = Net.COMPRESSION_320
        assertNotEquals(DEFAULT_AUDIO_COMPRESSION, expected)

        subject.setAudioCompression(expected)
        val actual = subject.audioCompressionFlow.first()

        assertEquals(expected, actual)
    }

    @Test
    @DisplayName("default ignore audio focus value is correct")
    fun getIgnoreAudioFocus_defaultValue() = runTest {
        val actual = subject.getIgnoreAudioFocus()

        assertEquals(DEFAULT_IGNORE_AUDIO_FOCUS, actual)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    @DisplayName("setIgnoreAudioFocus sets client port correctly")
    fun setIgnoreAudioFocus_updatesCorrectly(expected: Boolean) = runTest {
        subject.setIgnoreAudioFocus(expected)
        val actual = subject.getIgnoreAudioFocus()

        assertEquals(expected, actual)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    @DisplayName("settingsScreenPreferencesFlow updates ignoreAudioFocus")
    fun settingsScreenPreferencesFlow_updatesAudioCompression(expected: Boolean) = runTest {
        subject.setIgnoreAudioFocus(expected)
        val actual = subject.settingsScreenPreferencesFlow.first().ignoreAudioFocus

        assertEquals(expected, actual)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    @DisplayName("ignoreAudioFocusFlow updates")
    fun ignoreAudioFocusFlow_updates(expected: Boolean) = runTest {
        subject.setIgnoreAudioFocus(expected)
        val actual = subject.ignoreAudioFocusFlow.first()

        assertEquals(expected, actual)
    }
}
