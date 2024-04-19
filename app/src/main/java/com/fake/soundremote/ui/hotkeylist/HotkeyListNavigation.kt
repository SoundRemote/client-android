package com.fake.soundremote.ui.hotkeylist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

private const val HOTKEY_LIST_ROUTE = "hotkey_list"

fun NavController.navigateToHotkeyList() {
    navigate(HOTKEY_LIST_ROUTE)
}

fun NavGraphBuilder.hotkeyListScreen(
    onNavigateToHotkeyCreate: () -> Unit,
    onNavigateToHotkeyEdit: (hotkeyId: Int) -> Unit,
    onNavigateUp: () -> Unit,
    setFab: ((@Composable () -> Unit)?) -> Unit,
) {
    composable(HOTKEY_LIST_ROUTE) {
        val viewModel: HotkeyListViewModel = hiltViewModel()
        val state by viewModel.hotkeyListState.collectAsStateWithLifecycle()
        val lifecycleOwner = LocalLifecycleOwner.current
        HotkeyListScreen(
            state = state,
            onNavigateToHotkeyCreate = {
                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    onNavigateToHotkeyCreate()
                }
            },
            onNavigateToHotkeyEdit = { hotkeyId ->
                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    onNavigateToHotkeyEdit(hotkeyId)
                }
            },
            onDelete = { viewModel.deleteHotkey(it) },
            onChangeFavoured = { hotkeyId, favoured ->
                viewModel.changeFavoured(hotkeyId, favoured)
            },
            onMove = { fromIndex: Int, toIndex: Int ->
                viewModel.moveHotkey(fromIndex, toIndex)
            },
            onNavigateUp = onNavigateUp,
        )
        LaunchedEffect(Unit) {
            setFab(null)
        }
    }
}
