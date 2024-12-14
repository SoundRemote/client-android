package io.github.soundremote.ui.hotkey

import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import io.github.soundremote.util.ModKey
import kotlinx.serialization.Serializable

@Serializable
object HotkeyCreateRoute

@Serializable
data class HotkeyEditRoute(val hotkeyId: Int)

fun NavController.navigateToHotkeyCreate() {
    navigate(HotkeyCreateRoute)
}

fun NavController.navigateToHotkeyEdit(hotkeyId: Int) {
    navigate(HotkeyEditRoute(hotkeyId))
}

fun NavGraphBuilder.hotkeyCreateScreen(
    onNavigateUp: () -> Unit,
    showSnackbar: (String, SnackbarDuration) -> Unit,
    compactHeight: Boolean,
) {
    composable<HotkeyCreateRoute> {
        HotkeyScreenRoute(
            onNavigateUp = onNavigateUp,
            showSnackbar = showSnackbar,
            compactHeight = compactHeight
        )
    }
}

fun NavGraphBuilder.hotkeyEditScreen(
    onNavigateUp: () -> Unit,
    showSnackbar: (String, SnackbarDuration) -> Unit,
    compactHeight: Boolean,
) {
    composable<HotkeyEditRoute> { backStackEntry ->
        val route: HotkeyEditRoute = backStackEntry.toRoute()
        HotkeyScreenRoute(
            hotkeyId = route.hotkeyId,
            onNavigateUp = onNavigateUp,
            showSnackbar = showSnackbar,
            compactHeight = compactHeight
        )
    }
}

@Composable
private fun HotkeyScreenRoute(
    hotkeyId: Int? = null,
    onNavigateUp: () -> Unit,
    showSnackbar: (String, SnackbarDuration) -> Unit,
    compactHeight: Boolean,
) {
    val viewModel: HotkeyViewModel = hiltViewModel()
    hotkeyId?.let { viewModel.loadHotkey(it) }
    val state by viewModel.hotkeyScreenState.collectAsStateWithLifecycle()
    HotkeyScreen(
        state = state,
        onKeyCodeChange = { viewModel.updateKeyCode(it) },
        onWinChange = { viewModel.updateMod(ModKey.WIN, it) },
        onCtrlChange = { viewModel.updateMod(ModKey.CTRL, it) },
        onShiftChange = { viewModel.updateMod(ModKey.SHIFT, it) },
        onAltChange = { viewModel.updateMod(ModKey.ALT, it) },
        onNameChange = { viewModel.updateName(it) },
        checkCanSave = viewModel::canSave,
        onSave = viewModel::saveHotkey,
        onClose = onNavigateUp,
        showSnackbar = showSnackbar,
        compactHeight = compactHeight,
    )
}
