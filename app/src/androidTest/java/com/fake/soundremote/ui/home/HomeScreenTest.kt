package com.fake.soundremote.ui.home

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isOff
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.fake.soundremote.R
import com.fake.soundremote.stringResource
import com.fake.soundremote.ui.theme.SoundRemoteTheme
import com.fake.soundremote.util.ConnectionStatus
import com.fake.soundremote.util.Key
import com.fake.soundremote.util.HotkeyDescription
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

internal class HomeScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val navigateUp by composeTestRule.stringResource(R.string.navigate_up)
    private val muteApp by composeTestRule.stringResource(R.string.action_mute_app)
    private val unmuteApp by composeTestRule.stringResource(R.string.action_unmute_app)
    private val connect by composeTestRule.stringResource(R.string.connect_caption)
    private val disconnect by composeTestRule.stringResource(R.string.disconnect_caption)
    private val showRecentServers by composeTestRule.stringResource(R.string.action_recent_servers)
    private val recentServersTitle by composeTestRule.stringResource(R.string.recent_servers_title)
    private val mediaStop by composeTestRule.stringResource(R.string.key_media_stop)

    // Home screen should not contain navigate up arrow
    @Test
    fun navigateUp_doesNotExist() {
        composeTestRule.setContent {
            CreateHomeScreen()
        }

        composeTestRule.onNodeWithContentDescription(navigateUp).assertDoesNotExist()
    }

    // Mute button is not toggled when muted state is off
    @Test
    fun muteButton_stateNotMuted_notToggled() {
        val uiState = HomeUIState(isMuted = false)
        composeTestRule.setContent {
            CreateHomeScreen(uiState = uiState)
        }

        composeTestRule.onNodeWithContentDescription(muteApp).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(muteApp).assert(isOff())
    }

    // Mute button is toggled when muted state is on
    @Test
    fun muteButton_stateMuted_toggled() {
        val uiState = HomeUIState(isMuted = true)
        composeTestRule.setContent {
            CreateHomeScreen(uiState = uiState)
        }

        composeTestRule.onNodeWithContentDescription(unmuteApp).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(unmuteApp).assert(isOn())
    }

    // Click on mute button mutes
    @Test
    fun muteButton_click_mutes() {
        var actual = false
        composeTestRule.setContent {
            val uiState = HomeUIState(isMuted = false)
            CreateHomeScreen(uiState = uiState, onSetMuted = { actual = it })
        }

        composeTestRule.onNodeWithContentDescription(muteApp).performClick()

        assertTrue(actual)
    }

    // Click on unmute button unmutes
    @Test
    fun unmuteButton_click_unmutes() {
        var actual = true
        composeTestRule.setContent {
            val uiState = HomeUIState(isMuted = true)
            CreateHomeScreen(uiState = uiState, onSetMuted = { actual = it })
        }

        composeTestRule.onNodeWithContentDescription(unmuteApp).performClick()

        assertFalse(actual)
    }

    // Click on MediaBar button invokes callback
    @Test
    fun mediaButton_click_sendsKey() {
        var actual: Key? = null
        composeTestRule.setContent {
            CreateHomeScreen(onSendKey = { actual = it })
        }

        composeTestRule.onNodeWithContentDescription(mediaStop).performClick()

        assertEquals(Key.MEDIA_STOP, actual)
    }

    // Connect button is displayed when disconnected
    @Test
    fun connectButton_whenDisconnected_isDisplayed() {
        composeTestRule.setContent {
            val uiState = HomeUIState(
                connectionStatus = ConnectionStatus.DISCONNECTED,
            )
            CreateHomeScreen(uiState = uiState)
        }

        composeTestRule.onNodeWithContentDescription(connect).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(disconnect).assertDoesNotExist()
    }

    // Disconnect button is displayed when connected
    @Test
    fun disconnectButton_whenConnected_isDisplayed() {
        composeTestRule.setContent {
            val uiState = HomeUIState(
                connectionStatus = ConnectionStatus.CONNECTED,
            )
            CreateHomeScreen(uiState = uiState)
        }

        composeTestRule.onNodeWithContentDescription(disconnect).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(connect).assertDoesNotExist()
    }

    // Disconnect button is displayed when connecting
    @Test
    fun disconnectButton_whenConnecting_isDisplayed() {
        composeTestRule.setContent {
            val uiState = HomeUIState(
                connectionStatus = ConnectionStatus.CONNECTING,
            )
            CreateHomeScreen(uiState = uiState)
        }

        composeTestRule.onNodeWithContentDescription(disconnect).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(connect).assertDoesNotExist()
    }

    // Click on the connect button connects
    @Test
    fun connectButton_click_connects() {
        val expected = "123.45.67.89"
        var actual = ""
        composeTestRule.setContent {
            val uiState = HomeUIState(
                connectionStatus = ConnectionStatus.DISCONNECTED,
                serverAddress = expected,
            )
            CreateHomeScreen(uiState = uiState, onConnect = { actual = it })
        }

        composeTestRule.onNodeWithContentDescription(connect).performClick()

        assertEquals(expected, actual)
    }

    // Click on the disconnect button disconnects
    @Test
    fun disconnectButton_click_disconnects() {
        var actualPerformed = false
        composeTestRule.setContent {
            val uiState = HomeUIState(
                connectionStatus = ConnectionStatus.CONNECTED,
            )
            CreateHomeScreen(uiState = uiState, onDisconnect = { actualPerformed = true })
        }

        composeTestRule.onNodeWithContentDescription(disconnect).performClick()

        assertTrue(actualPerformed)
    }

    // Click on a hotkey calls onSendHotkey
    @Test
    fun hotkey_click_sendsHotkey() {
        val expectedId = 12
        val name = "Key Title"
        val description = HotkeyDescription.WithString("Key Description")
        val hotkey = HomeHotkeyUIState(expectedId, name, description)
        var actualId = -1
        composeTestRule.setContent {
            val uiState = HomeUIState(
                hotkeys = listOf(hotkey),
            )
            CreateHomeScreen(uiState = uiState, onSendHotkey = { actualId = it })
        }

        composeTestRule.onNodeWithText(name).performClick()

        assertEquals(expectedId, actualId)
    }

    // Long click on a hotkey calls onEditHotkey
    @Test
    fun hotkey_longClick_editsHotkey() {
        val expectedId = 12
        val name = "Key Title"
        val description = HotkeyDescription.WithString("Key Description")
        val hotkey = HomeHotkeyUIState(expectedId, name, description)
        var actualId = -1
        composeTestRule.setContent {
            val uiState = HomeUIState(
                hotkeys = listOf(hotkey),
            )
            CreateHomeScreen(uiState = uiState, onEditHotkey = { actualId = it })
        }

        composeTestRule.onNodeWithText(name).performTouchInput { longClick() }

        assertEquals(expectedId, actualId)
    }

    // Click on recent servers button shows recent servers dialog
    @Test
    fun recentServersButton_click_showsRecentServersDialog() {
        val recentServer = "123.45.67.89"
        composeTestRule.setContent {
            val uiState = HomeUIState(
                recentServersAddresses = listOf(recentServer),
            )
            CreateHomeScreen(uiState = uiState)
        }

        composeTestRule.onNodeWithContentDescription(showRecentServers).performClick()

        composeTestRule.onNodeWithText(recentServersTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(recentServer).assertIsDisplayed()
    }

    // Click on recent server updates server address edit
    @Test
    fun recentServer_click_updatesAddressEdit() {
        val recentServer = "123.45.67.89"
        composeTestRule.setContent {
            val uiState = HomeUIState(
                serverAddress = "",
                recentServersAddresses = listOf(recentServer),
            )
            CreateHomeScreen(uiState = uiState)
        }

        composeTestRule.onNodeWithContentDescription(showRecentServers).performClick()
        composeTestRule.onNodeWithText(recentServer).performClick()

        // Dialog is closed
        composeTestRule.onNodeWithText(recentServersTitle).assertDoesNotExist()
        // Clicked address is displayed
        composeTestRule.onNodeWithText(recentServer).assertIsDisplayed()
    }

    @Suppress("TestFunctionName")
    @Composable
    private fun CreateHomeScreen(
        modifier: Modifier = Modifier,
        uiState: HomeUIState = HomeUIState(),
        @StringRes messageId: Int? = null,
        onSendHotkey: (Int) -> Unit = {},
        onSendKey: (Key) -> Unit = {},
        onEditHotkey: (Int) -> Unit = {},
        onConnect: (String) -> Unit = {},
        onDisconnect: () -> Unit = {},
        onSetMuted: (Boolean) -> Unit = {},
        onMessageShown: () -> Unit = {},
        onNavigateToEvents: () -> Unit = {},
        onNavigateToSettings: () -> Unit = {},
        onNavigateToAbout: () -> Unit = {},
        showSnackbar: (String, SnackbarDuration) -> Unit = { _, _ -> },
    ) {
        SoundRemoteTheme {
            HomeScreen(
                uiState = uiState,
                messageId = messageId,
                onSendHotkey = onSendHotkey,
                onSendKey = onSendKey,
                onNavigateToEditHotkey = onEditHotkey,
                onConnect = onConnect,
                onDisconnect = onDisconnect,
                onSetMuted = onSetMuted,
                onMessageShown = onMessageShown,
                onNavigateToEvents = onNavigateToEvents,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToAbout = onNavigateToAbout,
                showSnackbar = showSnackbar,
                modifier = modifier,
                compactHeight = false,
            )
        }
    }
}
