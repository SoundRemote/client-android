package io.github.soundremote.ui.settings

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.soundremote.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SelectPreferenceTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val testOptions: List<SelectableOption<Int>> = List(100) {
        val id = it + 1
        SelectableOption(id, R.string.compression_320)
    }

    @Test
    fun selectedOption_notInOptionsList_nothingIsSelected() {
        val title = "Preference title"
        composeRule.setContent {
            CreateSelectPreference(
                selected = 1_234_456,
                title = title,
                options = testOptions,
            )
        }

        composeRule.apply {
            onNodeWithText(title).performClick()
            onAllNodes(isSelectable() and isSelected()).assertCountEquals(0)
        }
    }

    @Test
    fun selectedOption_inOptionsList_isSelected() {
        val title = "Preference title"
        val expectedValue = testOptions.last().value
        composeRule.setContent {
            CreateSelectPreference(
                selected = expectedValue,
                title = title,
                options = testOptions,
            )
        }

        composeRule.apply {
            onNodeWithText(title).performClick()
            onNodeWithTag("selectable:$expectedValue").assertIsSelected()
        }
    }

    @Test
    fun selectDialogOption_onClick_invokesCallback() {
        val title = "Preference title"
        val expectedValue = testOptions[1].value
        var actual = -1
        composeRule.setContent {
            CreateSelectPreference(
                selected = testOptions[0].value,
                title = title,
                options = testOptions,
                onSelect = { actual = it }
            )
        }

        composeRule.apply {
            onNodeWithText(title).performClick()
            onNodeWithTag("selectable:$expectedValue").apply {
                assertIsNotSelected()
                performClick()
            }
        }

        assertEquals(expectedValue, actual)
    }

    @Suppress("TestFunctionName")
    @Composable
    private fun <T : Any> CreateSelectPreference(
        selected: T,
        title: String = "Preference title",
        summary: String = "Preference summary",
        options: List<SelectableOption<T>> = emptyList(),
        onSelect: (T) -> Unit = {},
    ) {
        SelectPreference(
            title = title,
            summary = summary,
            options = options,
            selectedValue = selected,
            onSelect = onSelect,
        )
    }
}
