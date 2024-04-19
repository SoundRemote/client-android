package com.fake.soundremote.ui

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
import com.fake.soundremote.ui.about.aboutScreen
import com.fake.soundremote.ui.about.navigateToAbout
import com.fake.soundremote.ui.events.eventsScreen
import com.fake.soundremote.ui.events.navigateToEvents
import com.fake.soundremote.ui.home.homeRoute
import com.fake.soundremote.ui.home.homeScreen
import com.fake.soundremote.ui.hotkey.hotkeyCreateScreen
import com.fake.soundremote.ui.hotkey.hotkeyEditScreen
import com.fake.soundremote.ui.hotkey.navigateToHotkeyCreate
import com.fake.soundremote.ui.hotkey.navigateToHotkeyEdit
import com.fake.soundremote.ui.hotkeylist.hotkeyListScreen
import com.fake.soundremote.ui.hotkeylist.navigateToHotkeyList
import com.fake.soundremote.ui.settings.navigateToSettings
import com.fake.soundremote.ui.settings.settingsScreen

@Composable
fun AppNavigation(
    windowSizeClass: WindowSizeClass,
    showSnackbar: (String, SnackbarDuration) -> Unit,
    setFab: ((@Composable () -> Unit)?) -> Unit,
    padding: PaddingValues
) {
    val navController = rememberNavController()
    val compactHeight = windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact
    NavHost(
        navController = navController,
        startDestination = homeRoute,
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
            setFab = setFab,
            compactHeight = compactHeight,
        )
        hotkeyListScreen(
            onNavigateToHotkeyCreate = navController::navigateToHotkeyCreate,
            onNavigateToHotkeyEdit = { hotkeyId ->
                navController.navigateToHotkeyEdit(hotkeyId)
            },
            onNavigateUp = navController::navigateUp,
            setFab = setFab,
        )
        hotkeyCreateScreen(
            onNavigateUp = navController::navigateUp,
            showSnackbar = showSnackbar,
            setFab = setFab,
            compactHeight = compactHeight,
        )
        hotkeyEditScreen(
            onNavigateUp = navController::navigateUp,
            showSnackbar = showSnackbar,
            setFab = setFab,
            compactHeight = compactHeight,
        )
        eventsScreen(
            onNavigateUp = navController::navigateUp,
            setFab = setFab,
        )
        settingsScreen(
            onNavigateUp = navController::navigateUp,
            setFab = setFab,
        )
        aboutScreen(
            onNavigateUp = navController::navigateUp,
            setFab = setFab,
        )
    }
}
