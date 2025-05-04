package io.github.soundremote.ui.home

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.dropUnlessResumed
import io.github.soundremote.R
import io.github.soundremote.ui.components.ListItemHeadline
import io.github.soundremote.ui.components.ListItemSupport
import io.github.soundremote.ui.theme.SoundRemoteTheme
import io.github.soundremote.util.ConnectionStatus
import io.github.soundremote.util.HotkeyDescription
import io.github.soundremote.util.Key
import io.github.soundremote.util.TestTag

private val listItemPadding = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
private val hotkeyItemModifier = Modifier
    .fillMaxWidth()
    .height(72.dp)
    .then(listItemPadding)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUIState,
    @StringRes messageId: Int?,
    onSendHotkey: (hotkeyId: Int) -> Unit,
    onSendKey: (Key) -> Unit,
    onNavigateToEditHotkey: (hotkeyId: Int) -> Unit,
    onConnect: (address: String) -> Unit,
    onDisconnect: () -> Unit,
    onSetMuted: (muted: Boolean) -> Unit,
    onMessageShown: () -> Unit,
    onNavigateToHotkeyList: () -> Unit,
    onNavigateToEvents: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    showSnackbar: (String, SnackbarDuration) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Messages
    messageId?.let { id ->
        val message = stringResource(id)
        showSnackbar(message, SnackbarDuration.Short)
        onMessageShown()
    }

    var address by rememberSaveable(uiState.serverAddress, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(uiState.serverAddress))
    }
    val onAddressChange: (TextFieldValue) -> Unit = { newAddressValue ->
        cleanAddressInput(newAddressValue, address)?.let { address = it }
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AddressEdit(
                        address = address,
                        recentAddresses = uiState.recentServersAddresses,
                        onChange = onAddressChange,
                        onConnect = { onConnect(address.text) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 16.dp)
                    )
                },
                actions = {
                    ConnectButton(
                        connectionStatus = uiState.connectionStatus,
                        onConnect = { onConnect(address.text) },
                        onDisconnect = onDisconnect,
                    )
                    IconToggleButton(
                        checked = uiState.isMuted,
                        onCheckedChange = { onSetMuted(it) }
                    ) {
                        if (uiState.isMuted) {
                            Icon(
                                painter = painterResource(R.drawable.ic_volume_mute),
                                contentDescription = stringResource(R.string.action_unmute_app)
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_volume_up),
                                contentDescription = stringResource(R.string.action_mute_app)
                            )
                        }
                    }
                    Box {
                        var showMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.navigation_menu)
                            )
                        }
                        val lifecycleOwner = LocalLifecycleOwner.current
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.testTag(TestTag.NAVIGATION_MENU)
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_events)) },
                                onClick = dropUnlessResumed(lifecycleOwner) {
                                    showMenu = false
                                    onNavigateToEvents()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_settings)) },
                                onClick = dropUnlessResumed(lifecycleOwner) {
                                    showMenu = false
                                    onNavigateToSettings()
                                },
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_about)) },
                                onClick = dropUnlessResumed(lifecycleOwner) {
                                    showMenu = false
                                    onNavigateToAbout()
                                },
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = dropUnlessResumed {
                    onNavigateToHotkeyList()
                },
                modifier = Modifier
                    .padding(bottom = 48.dp),
            ) {
                Icon(Icons.Default.Edit, stringResource(R.string.action_edit_hotkeys))
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = modifier
            .testTag("homeScreen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .weight(1f),
            ) {
                items(items = uiState.hotkeys, key = { it.id }) { hotkey ->
                    HotkeyItem(
                        name = hotkey.name,
                        description = hotkey.description.asString(),
                        onClick = { onSendHotkey(hotkey.id) },
                        onLongClick = dropUnlessResumed {
                            onNavigateToEditHotkey(hotkey.id)
                        },
                    )
                }
            }
            MediaBar(onSendKey)
        }
    }
}

/**
 * Filter out leading zeroes and everything else except digits and dots
 */
private fun cleanAddressInput(newValue: TextFieldValue, oldValue: TextFieldValue): TextFieldValue? {
    if (newValue.text == oldValue.text) return newValue
    val newText = newValue.text.filter { it.isDigit() || it == '.' }.trimStart { it == '0' }
    if (newText != oldValue.text) return newValue.copy(text = newText)
    return null
}

@Composable
private fun AddressEdit(
    address: TextFieldValue,
    recentAddresses: List<String>,
    onChange: (TextFieldValue) -> Unit,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRecentServers by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = address,
        onValueChange = onChange,
        placeholder = { Text(stringResource(R.string.server_address)) },
        // To prevent font size change when TextField is in TopAppBar
        textStyle = LocalTextStyle.current.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Go,
            keyboardType = KeyboardType.Number,
        ),
        keyboardActions = KeyboardActions(onAny = { onConnect() }),
        trailingIcon = if (recentAddresses.isEmpty()) {
            null
        } else {
            {
                Icon(
                    Icons.Default.ArrowDropDown,
                    stringResource(R.string.action_recent_servers),
                    Modifier
                        .size(24.dp)
                        .rotate(if (showRecentServers) 180f else 0f)
                        .clickable { showRecentServers = !showRecentServers },
                )
            }
        },
        modifier = modifier
            // To prevent TextField size change when is in TopAppBar
            .height(56.dp)
    )
    if (showRecentServers) {
        AlertDialog(
            title = {
                Text(stringResource(R.string.recent_servers_title))
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    for (i in recentAddresses.indices.reversed()) {
                        ListItemHeadline(
                            text = recentAddresses[i],
                            modifier = Modifier
                                .clickable {
                                    onChange(TextFieldValue(recentAddresses[i]))
                                    showRecentServers = false
                                }
                                .height(56.dp)
                                .fillMaxWidth()
                                .then(listItemPadding),
                        )
                    }
                }
            },
            onDismissRequest = { showRecentServers = false },
            dismissButton = {
                TextButton(
                    onClick = { showRecentServers = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {},
        )
    }
}

@Composable
private fun ConnectButton(
    connectionStatus: ConnectionStatus,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connectedColor = colorResource(R.color.indicatorConnected)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        if (connectionStatus == ConnectionStatus.CONNECTING) {
            CircularProgressIndicator()
        }
        when (connectionStatus) {
            ConnectionStatus.DISCONNECTED -> {
                IconButton(
                    onClick = onConnect,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowForward,
                        contentDescription = stringResource(R.string.connect_caption),
                    )
                }
            }

            ConnectionStatus.CONNECTING,
            ConnectionStatus.CONNECTED -> {
                val tint = if (connectionStatus == ConnectionStatus.CONNECTED) {
                    connectedColor
                } else {
                    LocalContentColor.current
                }
                IconButton(
                    onClick = onDisconnect,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.disconnect_caption),
                        tint = tint
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HotkeyItem(
    name: String,
    description: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .then(hotkeyItemModifier),
        verticalArrangement = Arrangement.Center,
    ) {
        ListItemHeadline(text = name)
        ListItemSupport(text = description)
    }
}

@Preview(
    showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO, name = "Light",
    device = "id:Nexus 5"
)
@Preview(
    showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark",
    device = "id:Nexus 5"
)
@Composable
private fun Portrait() {
    HomePreview()
}

@Preview(
    showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO, name = "Light",
    device = "spec:parent=Nexus 5,orientation=landscape"
)
@Preview(
    showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark",
    device = "spec:parent=Nexus 5,orientation=landscape"
)
@Composable
private fun Landscape() {
    HomePreview()
}

@Composable
private fun HomePreview() {
    var status by remember { mutableStateOf(ConnectionStatus.DISCONNECTED) }
    var id = 0
    SoundRemoteTheme {
        HomeScreen(
            uiState = HomeUIState(
                serverAddress = "192.168.0.1",
                hotkeys = listOf(
                    HomeHotkeyUIState(
                        ++id,
                        "X",
                        HotkeyDescription.WithString("X")
                    ),
                    HomeHotkeyUIState(
                        ++id,
                        "Volume up",
                        HotkeyDescription.WithLabelId("Ctrl + Alt + ", R.string.key_delete)
                    ),
                ),
                connectionStatus = status,
                isMuted = true,
            ),
            messageId = null,
            onNavigateToEditHotkey = {},
            onConnect = { status = ConnectionStatus.CONNECTING },
            onDisconnect = {
                status = if (status == ConnectionStatus.CONNECTING) {
                    ConnectionStatus.CONNECTED
                } else {
                    ConnectionStatus.DISCONNECTED
                }
            },
            onSetMuted = {},
            onSendHotkey = {},
            onSendKey = {},
            onMessageShown = {},
            onNavigateToHotkeyList = {},
            onNavigateToEvents = {},
            onNavigateToSettings = {},
            onNavigateToAbout = {},
            showSnackbar = { _, _ -> },
        )
    }
}
