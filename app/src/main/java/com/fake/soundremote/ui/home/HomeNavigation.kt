package com.fake.soundremote.ui.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.fake.soundremote.R

const val homeRoute = "home"

fun NavGraphBuilder.homeScreen(
    onNavigateToHotkeyList: () -> Unit,
    onNavigateToEvents: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToEditHotkey: (hotkeyId: Int) -> Unit,
    showSnackbar: (String, SnackbarDuration) -> Unit,
    setFab: ((@Composable () -> Unit)?) -> Unit,
    compactHeight: Boolean,
) {
    composable(homeRoute) {
        val viewModel: HomeViewModel = hiltViewModel()
        val homeUIState by viewModel.homeUIState.collectAsStateWithLifecycle()
        val lifecycleOwner = LocalLifecycleOwner.current
        HomeScreen(
            uiState = homeUIState,
            messageId = viewModel.messageState,
            onNavigateToEditHotkey = {
                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    onNavigateToEditHotkey(it)
                }
            },
            onConnect = { viewModel.connect(it) },
            onDisconnect = viewModel::disconnect,
            onSendHotkey = { viewModel.sendHotkey(it) },
            onSendKey = { viewModel.sendKey(it) },
            onSetMuted = { viewModel.setMuted(it) },
            onMessageShown = viewModel::messageShown,
            onNavigateToEvents = {
                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    onNavigateToEvents.invoke()
                }
            },
            onNavigateToSettings = {
                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    onNavigateToSettings.invoke()
                }
            },
            onNavigateToAbout = {
                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    onNavigateToAbout.invoke()
                }
            },
            showSnackbar = showSnackbar,
            compactHeight = compactHeight,
        )
        LaunchedEffect(Unit) {
            setFab {
                FloatingActionButton(
                    onClick = {
                        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            onNavigateToHotkeyList.invoke()
                        }
                    },
                    modifier = Modifier
                        .padding(bottom = 48.dp),
                ) {
                    Icon(Icons.Default.Edit, stringResource(R.string.action_edit_hotkeys))
                }
            }
        }
    }
}
