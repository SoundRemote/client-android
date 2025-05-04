package io.github.soundremote.ui.hotkeylist

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.dropUnlessResumed
import io.github.soundremote.R
import io.github.soundremote.ui.components.ListItemHeadline
import io.github.soundremote.ui.components.ListItemSupport
import io.github.soundremote.ui.components.NavigateUpButton
import io.github.soundremote.util.TestTag
import java.io.Serializable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HotkeyListScreen(
    state: HotkeyListUIState,
    onNavigateToHotkeyCreate: () -> Unit,
    onNavigateToHotkeyEdit: (hotkeyId: Int) -> Unit,
    onDelete: (id: Int) -> Unit,
    onChangeFavoured: (id: Int, favoured: Boolean) -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
        TopAppBar(
            title = { Text(stringResource(R.string.hotkey_list_title)) },
            navigationIcon = { NavigateUpButton(onNavigateUp) },
            actions = {
                IconButton(
                    onClick = dropUnlessResumed {
                        onNavigateToHotkeyCreate()
                    },
                ) {
                    Icon(Icons.Default.Add, stringResource(R.string.action_hotkey_create))
                }
            },
            scrollBehavior = scrollBehavior,
        )
        HotkeyList(
            hotkeys = state.hotkeys,
            onChangeFavoured = onChangeFavoured,
            onEdit = onNavigateToHotkeyEdit,
            onMove = onMove,
            onDelete = onDelete,
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        )
    }
}

data class VisibleItemInfo(var index: Int, var offset: Int)
private data class DeleteInfo(val id: Int, val name: String) : Serializable

@Composable
private fun HotkeyList(
    hotkeys: List<HotkeyUIState>,
    onChangeFavoured: (Int, Boolean) -> Unit,
    onEdit: (Int) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onDelete: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var toDelete: DeleteInfo? by rememberSaveable { mutableStateOf(null) }

    /**
     * LazyList maintains scroll position based on items' ids, so when the first visible item is
     * moved list scrolls to it. This var remembers first visible item's information if it was
     * replaced by dragging.
     */
    var firstVisibleItem: VisibleItemInfo? by remember { mutableStateOf(null) }
    val listDragState = rememberListDragState(
        key = hotkeys,
        onMove = onMove,
        onFirstVisibleItemChange = { firstVisibleItem = it },
        listState = listState,
    )
    LaunchedEffect(hotkeys) {
        firstVisibleItem?.let {
            listState.scrollToItem(it.index, it.offset)
            firstVisibleItem = null
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
        ),
        modifier = modifier.fillMaxHeight()
    ) {
        itemsIndexed(
            items = hotkeys,
            key = { _, hotkey -> hotkey.id },
        ) { index, hotkeyState ->
            HotkeyItem(
                name = hotkeyState.name,
                description = hotkeyState.description.asString(),
                favoured = hotkeyState.favoured,
                onChangeFavoured = { onChangeFavoured(hotkeyState.id, it) },
                onEdit = dropUnlessResumed {
                    onEdit(hotkeyState.id)
                },
                onDelete = { toDelete = DeleteInfo(hotkeyState.id, hotkeyState.name) },
                onDragStart = { listDragState.onDragStart(index) },
                onDrag = { listDragState.onDrag(it) },
                onDragStop = { listDragState.onDragStop() },
                dragInfo = listDragState.dragInfo(index),
                isDragActive = listDragState.isDragActive,
            )
        }
    }
    if (toDelete != null) {
        val dismiss = { toDelete = null }
        val id = toDelete!!.id
        val name = toDelete!!.name
        AlertDialog(
            onDismissRequest = dismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(id)
                        dismiss()
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = dismiss
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = {
                Text(stringResource(R.string.hotkey_delete_confirmation))
            },
            text = {
                Text(
                    text = name,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                )
            }
        )
    }
}

@Composable
private fun HotkeyItem(
    name: String,
    description: String,
    favoured: Boolean,
    onChangeFavoured: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragStop: () -> Unit,
    dragInfo: DragInfo,
    isDragActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val animateOffset by animateFloatAsState(
        if (dragInfo.state == DragState.SHIFTED) dragInfo.offset else 0f,
        label = "Hotkey item drag"
    )
    val offsetY = when {
        !isDragActive -> 0f
        (dragInfo.state == DragState.DRAGGED) -> dragInfo.offset
        else -> animateOffset
    }
    val draggedElevation = 8.dp
    Surface(
        onClick = onEdit,
        modifier = modifier
            .height(72.dp)
            .zIndex(if (dragInfo.state == DragState.DRAGGED) 1f else 0f)
            .graphicsLayer(
                translationY = offsetY,
            ),
        tonalElevation = if (dragInfo.state == DragState.DRAGGED) draggedElevation else 0.dp,
        shadowElevation = if (dragInfo.state == DragState.DRAGGED) draggedElevation else 0.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Switch(
                checked = favoured,
                onCheckedChange = onChangeFavoured,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .testTag(TestTag.FAVOURITE_SWITCH)
            )
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                ListItemHeadline(name)
                ListItemSupport(description)
            }
            Icon(
                Icons.Default.Menu,
                contentDescription = stringResource(R.string.drag_handle_description),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .draggable(
                        state = rememberDraggableState { onDrag(it) },
                        orientation = Orientation.Vertical,
                        startDragImmediately = true,
                        onDragStarted = { onDragStart() },
                        onDragStopped = { onDragStop() },
                    )
            )
            Box {
                var showMenu by remember { mutableStateOf(false) }
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.hotkey_actions_menu_description)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit)) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CheckedItemPreview() {
    HotkeyItem(
        name = "Checked",
        description = "desc",
        favoured = true,
        onChangeFavoured = {},
        onEdit = {},
        onDelete = {},
        onDragStart = {},
        onDrag = {},
        onDragStop = {},
        dragInfo = DragInfo(),
        isDragActive = true,
    )
}

@Preview(showBackground = true)
@Composable
private fun UncheckedItemPreview() {
    HotkeyItem(
        name = "Unchecked",
        description = "desc",
        favoured = false,
        onChangeFavoured = {},
        onEdit = {},
        onDelete = {},
        onDragStart = {},
        onDrag = {},
        onDragStop = {},
        dragInfo = DragInfo(),
        isDragActive = true,
    )
}
