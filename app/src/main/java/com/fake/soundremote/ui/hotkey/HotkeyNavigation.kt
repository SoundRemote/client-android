package com.fake.soundremote.ui.hotkey

import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.fake.soundremote.util.ModKey

const val HOTKEY_ID_ARG = "hotkeyId"
private const val HOTKEY_CREATE_ROUTE = "hotkey_create"
private const val HOTKEY_EDIT_PREFIX = "hotkey_edit/"
private const val HOTKEY_EDIT_ROUTE = "$HOTKEY_EDIT_PREFIX{$HOTKEY_ID_ARG}"

fun NavController.navigateToHotkeyCreate() {
    navigate(HOTKEY_CREATE_ROUTE)
}

fun NavController.navigateToHotkeyEdit(hotkeyId: Int) {
    navigate("$HOTKEY_EDIT_PREFIX$hotkeyId")
}

fun NavGraphBuilder.hotkeyCreateScreen(
    onNavigateUp: () -> Unit,
    showSnackbar: (String, SnackbarDuration) -> Unit,
    setFab: ((@Composable () -> Unit)?) -> Unit,
    compactHeight: Boolean,
) {
    composable(HOTKEY_CREATE_ROUTE) {
        HotkeyScreenRoute(
            onNavigateUp = onNavigateUp,
            showSnackbar = showSnackbar,
            compactHeight = compactHeight
        )
        LaunchedEffect(Unit) {
            setFab(null)
        }
    }
}

internal class HotkeyEditArgs(val hotkeyId: Int?) {
    constructor(savedStateHandle: SavedStateHandle) :
            this(savedStateHandle.get<Int?>(HOTKEY_ID_ARG))
}

fun NavGraphBuilder.hotkeyEditScreen(
    onNavigateUp: () -> Unit,
    showSnackbar: (String, SnackbarDuration) -> Unit,
    setFab: ((@Composable () -> Unit)?) -> Unit,
    compactHeight: Boolean,
) {
    composable(
        route = HOTKEY_EDIT_ROUTE,
        arguments = listOf(navArgument(HOTKEY_ID_ARG) { type = NavType.IntType }),
    ) {
        HotkeyScreenRoute(
            onNavigateUp = onNavigateUp,
            showSnackbar = showSnackbar,
            compactHeight = compactHeight
        )
        LaunchedEffect(Unit) {
            setFab(null)
        }
    }
}

@Composable
private fun HotkeyScreenRoute(
    onNavigateUp: () -> Unit,
    showSnackbar: (String, SnackbarDuration) -> Unit,
    compactHeight: Boolean,
) {
    val viewModel: HotkeyViewModel = hiltViewModel()
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
