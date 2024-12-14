package io.github.soundremote.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import io.github.soundremote.ui.about.aboutScreen
import io.github.soundremote.ui.about.navigateToAbout
import io.github.soundremote.ui.events.eventsScreen
import io.github.soundremote.ui.events.navigateToEvents
import io.github.soundremote.ui.home.HomeRoute
import io.github.soundremote.ui.home.homeScreen
import io.github.soundremote.ui.hotkey.hotkeyCreateScreen
import io.github.soundremote.ui.hotkey.hotkeyEditScreen
import io.github.soundremote.ui.hotkey.navigateToHotkeyCreate
import io.github.soundremote.ui.hotkey.navigateToHotkeyEdit
import io.github.soundremote.ui.hotkeylist.hotkeyListScreen
import io.github.soundremote.ui.hotkeylist.navigateToHotkeyList
import io.github.soundremote.ui.settings.navigateToSettings
import io.github.soundremote.ui.settings.settingsScreen

@Composable
fun AppNavigation(
    windowSizeClass: WindowSizeClass,
    showSnackbar: (String, SnackbarDuration) -> Unit,
    padding: PaddingValues
) {
    val navController = rememberNavController()
    val compactHeight = windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        Modifier
            .padding(padding)
            .consumeWindowInsets(padding)
    ) {
        homeScreen(
            onNavigateToHotkeyList = navController::navigateToHotkeyList,
            onNavigateToEvents = navController::navigateToEvents,
            onNavigateToSettings = navController::navigateToSettings,
            onNavigateToAbout = navController::navigateToAbout,
            onNavigateToEditHotkey = { hotkeyId ->
                navController.navigateToHotkeyEdit(hotkeyId)
            },
            showSnackbar = showSnackbar,
            compactHeight = compactHeight,
        )
        hotkeyListScreen(
            onNavigateToHotkeyCreate = navController::navigateToHotkeyCreate,
            onNavigateToHotkeyEdit = { hotkeyId ->
                navController.navigateToHotkeyEdit(hotkeyId)
            },
            onNavigateUp = navController::navigateUp,
        )
        hotkeyCreateScreen(
            onNavigateUp = navController::navigateUp,
            showSnackbar = showSnackbar,
            compactHeight = compactHeight,
        )
        hotkeyEditScreen(
            onNavigateUp = navController::navigateUp,
            showSnackbar = showSnackbar,
            compactHeight = compactHeight,
        )
        eventsScreen(
            onNavigateUp = navController::navigateUp,
        )
        settingsScreen(
            onNavigateUp = navController::navigateUp,
        )
        aboutScreen(
            onNavigateUp = navController::navigateUp,
        )
    }
}
