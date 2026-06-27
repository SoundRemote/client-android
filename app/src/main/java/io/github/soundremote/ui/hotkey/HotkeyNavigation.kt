package io.github.soundremote.ui.hotkey

import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
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
    viewModel: HotkeyViewModel = hiltViewModel()
) {
    var needToLoadHotkey by rememberSaveable {
        mutableStateOf(hotkeyId != null)
    }
    LaunchedEffect(Unit) {
        if (needToLoadHotkey) {
            needToLoadHotkey = false
            hotkeyId?.let { viewModel.loadHotkey(it) }
        }
    }
    val state by viewModel.hotkeyScreenState.collectAsStateWithLifecycle()
    HotkeyScreen(
        state = state,
        onKeyCodeChange = { viewModel.updateKeyCode(it) },
        onModChange = viewModel::updateMod,
        onNameChange = { viewModel.updateName(it) },
        checkCanSave = viewModel::canSave,
        onSave = viewModel::saveHotkey,
        onClose = onNavigateUp,
        showSnackbar = showSnackbar,
        compactHeight = compactHeight,
    )
}
