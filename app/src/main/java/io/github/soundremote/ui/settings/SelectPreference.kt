package io.github.soundremote.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.soundremote.ui.components.ListItemHeadline

internal data class SelectableOption<T>(val value: T, @StringRes val textStringId: Int)

@Composable
internal fun <T : Any> SelectPreference(
    title: String,
    summary: String,
    options: List<SelectableOption<T>>,
    selectedValue: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    PreferenceItem(
        title = title,
        summary = summary,
        onClick = { showDialog = true },
        modifier = modifier,
    )
    if (showDialog) {
        val dismiss = { showDialog = false }
        AlertDialog(
            title = {
                Text(text = title)
            },
            text = {
                Options(
                    options = options,
                    selected = selectedValue,
                    onSelect = {
                        onSelect(it)
                        dismiss.invoke()
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = dismiss
                ) {
                    Text(stringResource(io.github.soundremote.R.string.cancel))
                }
            },
            onDismissRequest = dismiss,
        )
    }
}

@Composable
private fun <T : Any> Options(
    options: List<SelectableOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = rememberLazyListState(
            options.indexOfFirst { it.value == selected }
                .coerceAtLeast(0)
        ),
        modifier = modifier
            .padding(top = 8.dp, bottom = 8.dp),
    ) {
        items(
            items = options,
            key = { it.value },
        ) { option ->
            OptionItem(
                text = stringResource(option.textStringId),
                selected = option.value == selected,
                onClick = { onSelect(option.value) },
                modifier = Modifier.testTag("selectable:${option.value}"),
            )
        }
    }
}

@Composable
private fun OptionItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .height(56.dp)
            .fillMaxWidth()
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(16.dp))
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Spacer(Modifier.width(16.dp))
        ListItemHeadline(text = text)
    }
}
