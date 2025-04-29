package io.github.soundremote.ui.settings

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import io.github.soundremote.R
import io.github.soundremote.stringResource
import io.github.soundremote.ui.theme.SoundRemoteTheme
import io.github.soundremote.util.Net
import io.github.soundremote.util.Net.COMPRESSION_320
import io.github.soundremote.util.Net.COMPRESSION_NONE
import io.github.soundremote.util.TestTag
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

internal class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val navigateUp by composeTestRule.stringResource(R.string.navigate_up)
    private val advanced by composeTestRule.stringResource(R.string.settings_advanced_title)
    private val audioCompression by composeTestRule.stringResource(R.string.pref_compression_title)
    private val clientPort by composeTestRule.stringResource(R.string.pref_client_port_title)
    private val serverPort by composeTestRule.stringResource(R.string.pref_server_port_title)
    private val compressionNone by composeTestRule.stringResource(R.string.compression_none)
    private val compression320 by composeTestRule.stringResource(R.string.compression_320)
    private val ignoreFocus by composeTestRule.stringResource(R.string.pref_ignore_focus_title)
    private val ok by composeTestRule.stringResource(android.R.string.ok)

    // Settings screen should contain navigate up arrow
    @Test
    fun navigateUp_exist() {
        composeTestRule.setContent {
            CreateSettingsScreen()
        }

        composeTestRule.onNodeWithContentDescription(navigateUp).assertIsDisplayed()
    }

    // All (non advanced) settings exist by default
    @Test
    fun allSettings_byDefault_exist() {
        composeTestRule.setContent {
            CreateSettingsScreen()
        }

        composeTestRule.onNodeWithText(audioCompression).assertExists()
        composeTestRule.onNodeWithText(clientPort).assertExists()
        composeTestRule.onNodeWithText(serverPort).assertExists()
    }

    // Advanced settings do not exist by default
    @Test
    fun advancedSettings_byDefault_doesNotExist() {
        composeTestRule.setContent {
            CreateSettingsScreen()
        }

        composeTestRule.apply {
            onNodeWithText(ignoreFocus).assertDoesNotExist()
        }
    }

    @Test
    fun advancedSettings_onClickAdvanced_exist() {
        composeTestRule.setContent {
            CreateSettingsScreen()
        }

        composeTestRule.apply {
            onNodeWithText(advanced).apply {
                performScrollTo()
                performClick()
            }
            onNodeWithText(ignoreFocus).assertExists()
        }
    }

    // Current value display

    // Audio compression preference displays the current value if compression is on
    @Test
    fun audioCompressionOn_displaysCurrentValue() {
        @Net.Compression val value = COMPRESSION_320
        composeTestRule.setContent {
            CreateSettingsScreen(settings = SettingsUIState(audioCompression = value))
        }

        composeTestRule.onNodeWithText(this.audioCompression)
            .assertTextContains(compression320, true)
    }

    // Audio compression preference displays the current value if compression is off
    @Test
    fun audioCompressionOff_displaysCurrentValue() {
        @Net.Compression val value = COMPRESSION_NONE
        composeTestRule.setContent {
            CreateSettingsScreen(settings = SettingsUIState(audioCompression = value))
        }

        composeTestRule.onNodeWithText(audioCompression)
            .assertTextContains(compressionNone, true)
    }

    // Server port preference displays the current value
    @Test
    fun serverPort_displaysCurrentValue() {
        val value = 6789
        composeTestRule.setContent {
            CreateSettingsScreen(settings = SettingsUIState(serverPort = value))
        }

        composeTestRule.onNodeWithText(this.serverPort)
            .assertTextContains("$value", true)
    }

    // Client port preference displays the current value
    @Test
    fun clientPort_displaysCurrentValue() {
        val value = 5678
        composeTestRule.setContent {
            CreateSettingsScreen(settings = SettingsUIState(clientPort = value))
        }

        composeTestRule.onNodeWithText(this.clientPort)
            .assertTextContains("$value", true)
    }

    @Test
    fun ignoreFocus_displaysCurrentValue() {
        val expected = true
        composeTestRule.setContent {
            CreateSettingsScreen(settings = SettingsUIState(ignoreAudioFocus = expected))
        }

        composeTestRule.apply {
            // Click "Advanced"
            onNodeWithText(advanced).apply {
                performScrollTo()
                performClick()
            }
            onNodeWithText(ignoreFocus).assertIsOn()
        }
    }

    // On click

    // Click on audio compression preference should show selection dialog
    @Test
    fun audioCompression_onClick_showsSelectDialog() {
        composeTestRule.setContent {
            CreateSettingsScreen()
        }

        composeTestRule.onNodeWithText(audioCompression).apply {
            performScrollTo()
            performClick()
        }

        composeTestRule.onNode(
            hasText(audioCompression)
                    and hasAnyAncestor(isDialog())
        ).assertIsDisplayed()
    }

    // Click on server port preference should show edit dialog
    @Test
    fun serverPort_onClick_showsSelectDialog() {
        composeTestRule.setContent {
            CreateSettingsScreen()
        }
        composeTestRule.onNodeWithText(serverPort).apply {
            performScrollTo()
            performClick()
        }

        composeTestRule.onNode(
            hasText(serverPort)
                    and hasAnyAncestor(isDialog())
        ).assertIsDisplayed()
    }

    // Click on client port preference should show edit dialog
    @Test
    fun clientPort_onClick_showsSelectDialog() {
        composeTestRule.setContent {
            CreateSettingsScreen()
        }

        composeTestRule.onNodeWithText(clientPort).apply {
            performScrollTo()
            performClick()
        }

        composeTestRule.onNode(
            hasText(clientPort)
                    and hasAnyAncestor(isDialog())
        ).assertIsDisplayed()
    }

    // Update

    // Clicking on an audio compression option in the select dialog invokes update callback
    @Test
    fun audioCompressionOption_onClick_invokesCallback() {
        var actual = 0
        composeTestRule.setContent {
            CreateSettingsScreen(onSetAudioCompression = { actual = it })
        }

        composeTestRule.apply {
            onNodeWithText(audioCompression).apply {
                performScrollTo()
                performClick()
            }
            // Scroll the Lazy list with the compression options
            onNode(hasScrollAction() and hasAnyAncestor(isDialog()))
                .performScrollToNode(hasText(compression320))
            onNodeWithText(compression320).apply {
                assertIsNotSelected()
                performClick()
            }
        }

        assertEquals(COMPRESSION_320, actual)
    }

    // Clicking confirm button in edit server port dialog invokes update callback with correct value
    @Test
    fun serverPortEditDialog_onOk_invokesCallback() {
        var actual = 0
        val current = 10_000
        composeTestRule.setContent {
            CreateSettingsScreen(
                settings = SettingsUIState(serverPort = current),
                onSetServerPort = { actual = it })
        }

        val expected = 22_322
        composeTestRule.apply {
            onNodeWithText(serverPort).apply {
                performScrollTo()
                performClick()
            }
            onNodeWithTag(TestTag.INPUT_FIELD).performTextClearance()
            onNodeWithTag(TestTag.INPUT_FIELD).performTextInput("$expected")
            onNodeWithText(ok).performClick()
        }

        assertEquals(expected, actual)
    }

    // Clicking confirm button in edit client port dialog invokes update callback with correct value
    @Test
    fun clientPortEditDialog_onOk_invokesCallback() {
        var actual = 0
        val current = 10_000
        composeTestRule.setContent {
            CreateSettingsScreen(
                settings = SettingsUIState(clientPort = current),
                onSetClientPort = { actual = it })
        }

        val expected = 22_322
        composeTestRule.apply {
            onNodeWithText(clientPort).apply {
                performScrollTo()
                performClick()
            }
            onNodeWithTag(TestTag.INPUT_FIELD).performTextClearance()
            onNodeWithTag(TestTag.INPUT_FIELD).performTextInput("$expected")
            onNodeWithText(ok).performClick()
        }

        assertEquals(expected, actual)
    }

    @Test
    fun ignoreFocus_onClick_updatesPreference() {
        val initialValue = true
        val expected = !initialValue
        var updatedValue: Boolean? = null
        composeTestRule.setContent {
            CreateSettingsScreen(
                settings = SettingsUIState(ignoreAudioFocus = initialValue),
                onSetIgnoreAudioFocus = { updatedValue = it },
            )
        }

        composeTestRule.apply {
            onNodeWithText(advanced).apply {
                performScrollTo()
                performClick()
            }
            onNodeWithText(ignoreFocus).performClick()
        }
        assertEquals(expected, updatedValue)
    }

    @Suppress("TestFunctionName")
    @Composable
    private fun CreateSettingsScreen(
        modifier: Modifier = Modifier,
        settings: SettingsUIState = SettingsUIState(),
        onSetServerPort: (Int) -> Unit = {},
        onSetClientPort: (Int) -> Unit = {},
        onSetAudioCompression: (Int) -> Unit = {},
        onSetIgnoreAudioFocus: (Boolean) -> Unit = {},
        onNavigateUp: () -> Unit = {},
    ) {
        SoundRemoteTheme {
            SettingsScreen(
                settings = settings,
                onSetServerPort = onSetServerPort,
                onSetClientPort = onSetClientPort,
                onSetAudioCompression = onSetAudioCompression,
                onSetIgnoreAudioFocus = onSetIgnoreAudioFocus,
                onNavigateUp = onNavigateUp,
                modifier = modifier,
            )
        }
    }
}
