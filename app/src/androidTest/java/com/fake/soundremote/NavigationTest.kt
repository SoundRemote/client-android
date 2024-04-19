package com.fake.soundremote

import androidx.annotation.StringRes
import androidx.compose.ui.test.MainTestClock
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.fake.soundremote.util.TestTag
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.properties.ReadOnlyProperty

@HiltAndroidTest
class NavigationTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @BindValue
    @get:Rule(order = 1)
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private fun AndroidComposeTestRule<*, *>.stringResource(@StringRes resId: Int) =
        ReadOnlyProperty<Any?, String> { _, _ -> activity.getString(resId) }

    private val appName by composeTestRule.stringResource(R.string.app_name)

    // Screen titles
    private val homeTitle by composeTestRule.stringResource(R.string.app_name)
    private val eventsTitle by composeTestRule.stringResource(R.string.event_list_title)
    private val settingsTitle by composeTestRule.stringResource(R.string.settings_title)
    private val aboutTitleTemplate by composeTestRule.stringResource(R.string.about_title_template)
    private val hotkeysTitle by composeTestRule.stringResource(R.string.hotkey_list_title)
    private val createHotkeyTitle by composeTestRule.stringResource(R.string.hotkey_create_title)

    private val navigationMenu by composeTestRule.stringResource(R.string.navigation_menu)
    private val menuEvents by composeTestRule.stringResource(R.string.action_events)
    private val menuSettings by composeTestRule.stringResource(R.string.action_settings)
    private val menuAbout by composeTestRule.stringResource(R.string.action_about)
    private val editHotkeys by composeTestRule.stringResource(R.string.action_edit_hotkeys)
    private val createHotkey by composeTestRule.stringResource(R.string.action_hotkey_create)


    // First screen is HomeScreen
    @Test
    fun firstScreen_isHomeScreen() {
        composeTestRule.onNodeWithText(homeTitle).assertIsDisplayed()
    }

    // HomeScreen navigation menu button opens navigation menu
    @Test
    fun navigationMenuButton_onClick_showsNavigationMenu() {
        composeTestRule.apply {
            onNodeWithContentDescription(navigationMenu).performClick()
            onNodeWithTag(TestTag.NAVIGATION_MENU).assertIsDisplayed()
            onNodeWithText(menuEvents).assertIsDisplayed()
            onNodeWithText(menuSettings).assertIsDisplayed()
            onNodeWithText(menuAbout).assertIsDisplayed()
        }
    }

    // Menu Events navigates to events screen
    @Test
    fun menuEvents_onClick_navigatesToEventsScreen() {
        composeTestRule.apply {
            onNodeWithContentDescription(navigationMenu).performClick()
            onNodeWithText(menuEvents).performClick()
            onNodeWithText(eventsTitle).assertIsDisplayed()
        }
    }

    // Back from events screen returns to home screen
    @Test
    fun eventsScreen_back_returnsToHomeScreen() {
        composeTestRule.apply {
            onNodeWithContentDescription(navigationMenu).performClick()
            onNodeWithText(menuEvents).performClick()
            onNodeWithText(eventsTitle).assertIsDisplayed()
        }
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.onNodeWithText(homeTitle).assertIsDisplayed()
    }

    // Menu Settings navigates to settings screen
    @Test
    fun menuSettings_onClick_navigatesToSettingsScreen() {
        composeTestRule.apply {
            onNodeWithContentDescription(navigationMenu).performClick()
            onNodeWithText(menuSettings).performClick()
            onNodeWithText(settingsTitle).assertIsDisplayed()
        }
    }

    // Back from settings screen returns to home screen
    @Test
    fun settingsScreen_back_returnsToHomeScreen() {
        composeTestRule.apply {
            onNodeWithContentDescription(navigationMenu).performClick()
            onNodeWithText(menuSettings).performClick()
            onNodeWithText(settingsTitle).assertIsDisplayed()
        }
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.onNodeWithText(homeTitle).assertIsDisplayed()
    }

    // Menu About navigates to about screen
    @Test
    fun menuAbout_onClick_navigatesToAboutScreen() {
        composeTestRule.apply {
            onNodeWithContentDescription(navigationMenu).performClick()
            onNodeWithText(menuAbout).performClick()
            val screenTitle = aboutTitleTemplate.format(appName)
            onNodeWithText(screenTitle).assertIsDisplayed()
        }
    }

    // Back from about screen returns to home screen
    @Test
    fun aboutScreen_back_returnsToHomeScreen() {
        composeTestRule.apply {
            onNodeWithContentDescription(navigationMenu).performClick()
            onNodeWithText(menuAbout).performClick()
            val screenTitle = aboutTitleTemplate.format(appName)
            onNodeWithText(screenTitle).assertIsDisplayed()
        }
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.onNodeWithText(homeTitle).assertIsDisplayed()
    }

    // Edit hotkeys FAB navigates to hotkeys screen
    @Test
    fun editHotkeysFab_click_navigatesToHomeScreen() {
        composeTestRule.onNodeWithContentDescription(editHotkeys).performClick()
        composeTestRule.onNodeWithText(hotkeysTitle).assertIsDisplayed()
    }

    // Back from hotkeys screen returns to home screen
    @Test
    fun hotkeysScreen_back_returnsToHomeScreen() {
        composeTestRule.apply {
            onNodeWithContentDescription(editHotkeys).performClick()
            onNodeWithText(hotkeysTitle).assertIsDisplayed()
        }
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.onNodeWithText(homeTitle).assertIsDisplayed()
    }

    // Given: simultaneous clicks on different navigation menu items on `Home` screen.
    // Clicking back on opened screen should return to `Home` screen.
    // I.e. app should not navigate to other screens more than once.
    @Test
    fun navigateMenu_clickTwoItemsSimultaneously_navigatesOnce() {
        composeTestRule.apply {
            // Open navigation menu
            onNodeWithContentDescription(navigationMenu).performClick()
            // Click 2 menu items simultaneously
            onNodeWithText(menuAbout).performClick()
            mainClock.autoAdvance = false
            onNodeWithText(menuSettings).performClick()
            mainClock.autoAdvance = true
        }
        // Press back
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        // Should be on `Home` screen
        composeTestRule.onNodeWithText(homeTitle).assertIsDisplayed()
    }

    // Given: multiple simultaneous clicks on `Edit hotkeys` FAB on `Home` screen.
    // Clicking back on `Hotkeys` screen should return to `Home` screen.
    // I.e. app should not navigate to `Hotkeys` screens more than once.
    @Test
    fun multipleClicksEditHotkeys_back_returnsToHomeScreen() {
        composeTestRule.apply {
            // Imitate double click on `Edit hotkeys` button
            onNodeWithContentDescription(editHotkeys)
                .performSimultaneousDoubleClick(composeTestRule.mainClock)
        }
        // Press back
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        // Should be on `Home` screen
        composeTestRule.onNodeWithText(homeTitle).assertIsDisplayed()
    }

    // Create hotkey button navigates to create hotkey screen
    @Test
    fun createHotkey_click_navigatesToCreateHotkeyScreen() {
        composeTestRule.apply {
            onNodeWithContentDescription(editHotkeys).performClick()
            onNodeWithContentDescription(createHotkey).performClick()
        }
        composeTestRule.onNodeWithText(createHotkeyTitle).assertIsDisplayed()
    }

    // Back from create hotkey screen returns to hotkeys screen
    @Test
    fun createHotkey_back_returnsToHotkeysScreen() {
        composeTestRule.apply {
            onNodeWithContentDescription(editHotkeys).performClick()
            onNodeWithContentDescription(createHotkey).performClick()
            onNodeWithText(createHotkeyTitle).assertIsDisplayed()
        }
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.onNodeWithText(hotkeysTitle).assertIsDisplayed()
    }

    // Given: multiple simultaneous clicks on `create hotkey` button on `Hotkeys` screen.
    // Clicking back on `Create hotkey` screen should return to `Hotkeys` screen
    // I.e. app should not navigate to `Create hotkey` screen more than once.
    @Test
    fun multipleClicksCreateHotkey_back_returnsToHotkeysScreen() {
        composeTestRule.apply {
            // Go to `Hotkeys` screen
            onNodeWithContentDescription(editHotkeys).performClick()

            // Imitate double click on `Create hotkey` button
            onNodeWithContentDescription(createHotkey)
                .performSimultaneousDoubleClick(composeTestRule.mainClock)
        }
        // Press back
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        // Assert that app is back on `Hotkeys` screen
        composeTestRule.onNodeWithText(hotkeysTitle).assertIsDisplayed()
    }
}

private fun SemanticsNodeInteraction.performSimultaneousDoubleClick(clock: MainTestClock) {
    performClick()
    clock.autoAdvance = false
    performClick()
    clock.autoAdvance = true
}
