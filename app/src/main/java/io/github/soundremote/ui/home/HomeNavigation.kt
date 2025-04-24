package io.github.soundremote.ui.home

import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

fun NavGraphBuilder.homeScreen(
    onNavigateToHotkeyList: () -> Unit,
    onNavigateToEvents: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToEditHotkey: (hotkeyId: Int) -> Unit,
    showSnackbar: (String, SnackbarDuration) -> Unit,
) {
    composable<HomeRoute> {
        val viewModel: HomeViewModel = hiltViewModel()
        val homeUIState by viewModel.homeUIState.collectAsStateWithLifecycle()
        HomeScreen(
            uiState = homeUIState,
            messageId = viewModel.messageState,
            onNavigateToEditHotkey = { onNavigateToEditHotkey(it) },
            onConnect = { viewModel.connect(it) },
            onDisconnect = viewModel::disconnect,
            onSendHotkey = { viewModel.sendHotkey(it) },
            onSendKey = { viewModel.sendKey(it) },
            onSetMuted = { viewModel.setMuted(it) },
            onMessageShown = viewModel::messageShown,
            onNavigateToHotkeyList = onNavigateToHotkeyList,
            onNavigateToEvents = onNavigateToEvents,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToAbout = onNavigateToAbout,
            showSnackbar = showSnackbar,
        )
    }
}
