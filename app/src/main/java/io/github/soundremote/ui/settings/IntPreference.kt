package io.github.soundremote.ui.settings

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import io.github.soundremote.R
import io.github.soundremote.util.TestTag

@Composable
internal fun IntPreference(
    title: String,
    summary: String,
    value: Int,
    onPreferenceChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    validValues: IntRange? = null,
    defaultValue: Int? = null,
) {
    var showEdit by rememberSaveable { mutableStateOf(false) }
    val summaryText = if (defaultValue == null) {
        stringResource(R.string.pref_summary_template_short).format(value, summary)
    } else {
        val defaultValueText = stringResource(R.string.pref_default_value_template)
            .format(defaultValue)
        stringResource(R.string.pref_summary_template)
            .format(value, summary, defaultValueText)
    }

    PreferenceItem(
        title = title,
        summary = summaryText,
        onClick = { showEdit = true },
        modifier = modifier,
    )
    if (showEdit) {
        var editValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue(value.toString()))
        }
        val isValidValue by remember {
            derivedStateOf {
                validValues?.contains(editValue.text.toIntOrNull()) != false
            }
        }
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text(title) },
            text = {
                val editFocusRequester = remember { FocusRequester() }
                SideEffect {
                    editFocusRequester.requestFocus()
                }
                TextField(
                    value = editValue,
                    onValueChange = { newEditValue ->
                        editValue = cleanUIntInput(newEditValue, editValue) ?: return@TextField
                    },
                    supportingText = {
                        if (validValues != null) {
                            Text(
                                stringResource(R.string.pref_valid_int_range_template)
                                    .format(validValues.first, validValues.last)
                            )
                        }
                    },
                    isError = !isValidValue,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .focusRequester(editFocusRequester)
                        .testTag(TestTag.INPUT_FIELD)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onPreferenceChange(editValue.text.toInt())
                        showEdit = false
                    },
                    enabled = isValidValue,
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEdit = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

private fun cleanUIntInput(newValue: TextFieldValue, oldValue: TextFieldValue): TextFieldValue? {
    if (newValue.text == oldValue.text) return newValue
    val newText = newValue.text.filter { it.isDigit() }.trimStart { it == '0' }
    if (newText != oldValue.text) return newValue.copy(text = newText)
    return null
}

@Preview(showBackground = true)
@Composable
private fun IntPreferencePreview() {
    IntPreference(
        title = "Title",
        summary = "This is a very, very long and descriptive summary.",
        value = 1337,
        onPreferenceChange = {},
        defaultValue = 8976,
    )
}

@Preview(showBackground = true)
@Composable
private fun IntPreferenceNoDefaultPreview() {
    IntPreference(
        title = "Title",
        summary = "This is a very, very long and descriptive summary.",
        value = 1337,
        onPreferenceChange = {},
    )
}
