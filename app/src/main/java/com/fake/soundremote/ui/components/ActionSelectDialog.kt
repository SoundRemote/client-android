package com.fake.soundremote.ui.components

import androidx.annotation.StringRes
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fake.soundremote.R
import com.fake.soundremote.data.Action
import com.fake.soundremote.data.ActionType
import com.fake.soundremote.data.AppAction
import com.fake.soundremote.util.HotkeyDescription

@Composable
internal fun ActionSelectDialog(
    availableActionTypes: Set<ActionType>,
    initialAction: Action? = null,
    onConfirm: (Action?) -> Unit,
    onDismiss: () -> Unit,
    viewModel: HotkeySelectViewModel = hiltViewModel()
) {
    val hotkeys by viewModel.hotkeysState.collectAsStateWithLifecycle()

    ActionSelectDialog(
        availableActionTypes,
        initialAction,
        hotkeys,
        onConfirm,
        onDismiss,
    )
}

sealed interface ActionUIState {
    val id: Int

    data class WithHotkey(
        override val id: Int,
        val name: String,
        val description: HotkeyDescription,
    ) : ActionUIState

    data class WithStringIds(
        override val id: Int,
        @StringRes
        val nameId: Int,
        @StringRes
        val descriptionId: Int?,
    ) : ActionUIState
}

val appActions: List<ActionUIState> by lazy {
    AppAction.entries.map { appAction ->
        ActionUIState.WithStringIds(appAction.id, appAction.nameStringId, null)
    }
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@Composable
internal fun ActionSelectDialog(
    availableActionTypes: Set<ActionType>,
    initialAction: Action?,
    hotkeys: List<HotkeyInfoUIState>,
    onConfirm: (Action?) -> Unit,
    onDismiss: () -> Unit,
) {
    val actionTypes = rememberSaveable(availableActionTypes) {
        availableActionTypes.sortedBy { it.id }
    }
    // Action type selected in UI
    var selectedActionTypeIndex: Int by rememberSaveable {
        val index = if (initialAction == null) {
            0
        } else {
            actionTypes.indexOf(initialAction.type)
        }
        mutableIntStateOf(index)
    }
    // Action selected by user
    var selectedAction by rememberSaveable { mutableStateOf(initialAction) }
    // Action id in currently selected action type.
    // If selected ActionType is not the same as the currently selected Action's type, set to -1 so
    // nothing would be selected.
    val selectedActionId: Int? = remember(selectedActionTypeIndex, selectedAction) {
        val currentAction = selectedAction
        if (currentAction == null) {
            null
        } else {
            if (actionTypes[selectedActionTypeIndex] == currentAction.type) {
                currentAction.id
            } else {
                -1
            }
        }
    }
    val hotkeyActions = rememberSaveable(hotkeys) {
        hotkeys.map { hotkey ->
            ActionUIState.WithHotkey(
                hotkey.id,
                hotkey.name,
                hotkey.description,
            )
        }
    }
    val items = when (actionTypes[selectedActionTypeIndex]) {
        ActionType.APP -> appActions
        ActionType.HOTKEY -> hotkeyActions
    }

    AlertDialog(
        title = {
            Text(stringResource(R.string.action_select_title))
        },
        text = {
            Column {
                TabRow(selectedTabIndex = selectedActionTypeIndex) {
                    for ((index, actionType) in actionTypes.withIndex()) {
                        Tab(
                            selected = selectedActionTypeIndex == index,
                            onClick = { selectedActionTypeIndex = index },
                            text = { Text(stringResource(actionType.nameStringId)) }
                        )
                    }
                }
                ActionList(
                    items = items,
                    selectedActionId = selectedActionId,
                    onSelect = { id ->
                        selectedAction = if (id == null) {
                            null
                        } else {
                            Action(actionTypes[selectedActionTypeIndex], id)
                        }
                    },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedAction) }
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
private fun ActionList(
    items: List<ActionUIState>,
    selectedActionId: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()

    var prescrolled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(items) {
        // Scroll to initially selected action only once on dialog open
        if (prescrolled || items.isEmpty()) return@LaunchedEffect
        // Add 1 to account for the `No action` option
        val index = items.indexOfFirst { it.id == selectedActionId } + 1
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
            ActionItem(
                name = stringResource(R.string.action_none),
                description = null,
                selected = selectedActionId == null,
                onClick = { onSelect(null) },
            )
        }
        items(
            items = items,
            key = { it.id }
        ) { action ->
            val (name, description) = when (action) {
                is ActionUIState.WithHotkey -> {
                    action.name to action.description.asString()
                }

                is ActionUIState.WithStringIds -> {
                    stringResource(action.nameId) to
                            action.descriptionId?.let { stringResource(it) }
                }
            }
            ActionItem(
                name = name,
                description = description,
                selected = action.id == selectedActionId,
                onClick = { onSelect(action.id) }
            )
        }
    }
}

@Composable
private fun ActionItem(
    name: String,
    description: String?,
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
            .height(if (description == null) 56.dp else 72.dp)
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
            if (!description.isNullOrBlank()) {
                ListItemSupport(text = description)
            }
        }
    }
}
