package io.github.soundremote.ui.hotkeylist

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
) {
    composable<HotkeyListRoute> {
        val viewModel: HotkeyListViewModel = hiltViewModel()
        val state by viewModel.hotkeyListState.collectAsStateWithLifecycle()
        HotkeyListScreen(
            state = state,
            onNavigateToHotkeyCreate = onNavigateToHotkeyCreate,
            onNavigateToHotkeyEdit = onNavigateToHotkeyEdit,
            onDelete = { viewModel.deleteHotkey(it) },
            onChangeFavoured = { hotkeyId, favoured ->
                viewModel.changeFavoured(hotkeyId, favoured)
            },
            onMove = { fromIndex: Int, toIndex: Int ->
                viewModel.moveHotkey(fromIndex, toIndex)
            },
            onNavigateUp = onNavigateUp,
        )
    }
}
