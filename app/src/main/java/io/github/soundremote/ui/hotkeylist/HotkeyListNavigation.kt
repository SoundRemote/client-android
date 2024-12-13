package io.github.soundremote.ui.hotkeylist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
object HotkeyListRoute

fun NavController.navigateToHotkeyList() {
    navigate(HotkeyListRoute)
}

fun NavGraphBuilder.hotkeyListScreen(
    onNavigateToHotkeyCreate: () -> Unit,
    onNavigateToHotkeyEdit: (hotkeyId: Int) -> Unit,
    onNavigateUp: () -> Unit,
    setFab: ((@Composable () -> Unit)?) -> Unit,
) {
    composable<HotkeyListRoute> {
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
