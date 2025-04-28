package io.github.soundremote.ui.settings

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.soundremote.R
import io.github.soundremote.ui.components.ListItemHeadline
import io.github.soundremote.ui.components.NavigateUpButton
import io.github.soundremote.ui.theme.SoundRemoteTheme
import io.github.soundremote.util.DEFAULT_CLIENT_PORT
import io.github.soundremote.util.DEFAULT_SERVER_PORT
import io.github.soundremote.util.Net

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    settings: SettingsUIState,
    onSetServerPort: (Int) -> Unit,
    onSetClientPort: (Int) -> Unit,
    onSetAudioCompression: (Int) -> Unit,
    onSetIgnoreAudioFocus: (Boolean) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val validPorts = 1024..49151
    val compressionOptions = remember {
        compressionOptions()
    }
    val compressionSummaryId = remember(settings.audioCompression) {
        compressionOptions.find { it.value == settings.audioCompression }?.textStringId
    }

    Column(modifier) {
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
        TopAppBar(
            title = { Text(stringResource(R.string.settings_title)) },
            navigationIcon = { NavigateUpButton(onNavigateUp) },
            scrollBehavior = scrollBehavior,
        )
        Column(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
        ) {
            SelectPreference(
                title = stringResource(R.string.pref_compression_title),
                summary = if (compressionSummaryId == null) {
                    ""
                } else {
                    stringResource(compressionSummaryId)
                },
                options = compressionOptions,
                selectedValue = settings.audioCompression,
                onSelect = onSetAudioCompression,
            )
            IntPreference(
                title = stringResource(R.string.pref_server_port_title),
                summary = stringResource(R.string.pref_server_port_summary),
                value = settings.serverPort,
                onPreferenceChange = onSetServerPort,
                validValues = validPorts,
                defaultValue = DEFAULT_SERVER_PORT,
            )
            IntPreference(
                title = stringResource(R.string.pref_client_port_title),
                summary = stringResource(R.string.pref_client_port_summary),
                value = settings.clientPort,
                onPreferenceChange = onSetClientPort,
                validValues = validPorts,
                defaultValue = DEFAULT_CLIENT_PORT,
            )
            var showAdvanced by remember { mutableStateOf(false) }
            if (showAdvanced) {
                BooleanPreference(
                    title = stringResource(R.string.pref_ignore_focus_title),
                    summary = stringResource(R.string.pref_ignore_focus_summary),
                    value = settings.ignoreAudioFocus,
                    onPreferenceChange = onSetIgnoreAudioFocus,
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable(onClick = { showAdvanced = true })
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        ListItemHeadline(stringResource(R.string.settings_advanced_title))
                    }
                    Icon(Icons.Default.KeyboardArrowDown, null)
                }
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.safeDrawing))
        }
    }
}

private fun compressionOptions(): List<SelectableOption<Int>> = listOf(
    SelectableOption(Net.COMPRESSION_NONE, R.string.compression_none),
    SelectableOption(Net.COMPRESSION_64, R.string.compression_64),
    SelectableOption(Net.COMPRESSION_128, R.string.compression_128),
    SelectableOption(Net.COMPRESSION_192, R.string.compression_192),
    SelectableOption(Net.COMPRESSION_256, R.string.compression_256),
    SelectableOption(Net.COMPRESSION_320, R.string.compression_320),
)

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO, name = "Light")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark")
@Preview(
    showBackground = true,
    locale = "ru",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    name = "Light RU",
)
@Composable
private fun SettingsScreenPreview() {
    SoundRemoteTheme {
        SettingsScreen(
            settings = SettingsUIState(1234, 5678, 0),
            onSetClientPort = {},
            onSetServerPort = {},
            onSetAudioCompression = {},
            onSetIgnoreAudioFocus = {},
            onNavigateUp = {},
        )
    }
}
