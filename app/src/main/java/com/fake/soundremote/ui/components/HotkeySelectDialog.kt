package com.fake.soundremote.ui.components

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fake.soundremote.R
import com.fake.soundremote.util.HotkeyDescription

@Composable
internal fun HotkeySelectDialog(
    title: String,
    initialHotkeyId: Int? = null,
    onConfirm: (Int?) -> Unit,
    onDismiss: () -> Unit,
    viewModel: HotkeySelectViewModel = hiltViewModel()
) {
    val hotkeys by viewModel.hotkeysState.collectAsStateWithLifecycle()

    HotkeySelectDialog(
        title = title,
        items = hotkeys,
        initialHotkeyId = initialHotkeyId,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@Composable
internal fun HotkeySelectDialog(
    title: String,
    items: List<HotkeyInfoUIState>,
    initialHotkeyId: Int?,
    onConfirm: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedHotkeyId by rememberSaveable { mutableStateOf(initialHotkeyId) }

    AlertDialog(
        title = {
            Text(text = title)
        },
        text = {
            HotkeyList(
                items = items,
                selectedHotkeyId = selectedHotkeyId,
                onSelect = { id -> selectedHotkeyId = id },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedHotkeyId) }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}

@Composable
private fun HotkeyList(
    items: List<HotkeyInfoUIState>,
    selectedHotkeyId: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()

    var prescrolled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(items) {
        // Scroll to initially selected hotkey only once on dialog open
        if (prescrolled || items.isEmpty()) return@LaunchedEffect
        // Add 1 to account for the "No hotkey" option
        val index = items.indexOfFirst { it.id == selectedHotkeyId } + 1
        lazyListState.scrollToItem(index)
        prescrolled = true
    }

    LazyColumn(
        modifier = modifier
            .wrapContentHeight()
            .padding(top = 8.dp, bottom = 8.dp),
        state = lazyListState
    ) {
        item(key = null) {
            HotkeyItem(
                name = stringResource(R.string.hotkey_select_none),
                description = "",
                selected = selectedHotkeyId == null,
                onClick = { onSelect(null) },
            )
        }
        items(
            items = items,
            key = { hotkey -> hotkey.id }
        ) { hotkey ->
            HotkeyItem(
                name = hotkey.name,
                description = hotkey.description.asString(),
                selected = hotkey.id == selectedHotkeyId,
                onClick = { onSelect(hotkey.id) }
            )
        }
    }
}

@Composable
private fun HotkeyItem(
    name: String,
    description: String,
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
            .height(72.dp)
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
        Column {
            ListItemHeadline(text = name)
            if (description.isNotBlank()) {
                ListItemSupport(text = description)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HotkeyItemPreview() {
    HotkeyItem(
        name = "Hotkey name",
        description = "Description",
        selected = true,
        onClick = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun HotkeyItemNoDescriptionPreview() {
    HotkeyItem(
        name = "Hotkey name",
        description = "",
        selected = true,
        onClick = {}
    )
}

@Preview(showBackground = true, device = "id:Nexus S")
@Composable
private fun HotkeyDialogPreview() {
    HotkeySelectDialog(
        title = "Select Hotkey",
        items = List(10) {
            HotkeyInfoUIState(
                it,
                "Hotkey $it",
                HotkeyDescription.WithString("Ctrl + Shift + $it")
            )
        },
        initialHotkeyId = 9,
        onConfirm = {},
        onDismiss = {},
    )
}
