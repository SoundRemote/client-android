package com.fake.soundremote.ui.keystrokelist

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.fake.soundremote.R
import com.fake.soundremote.ui.util.ListItemHeadline
import com.fake.soundremote.ui.util.ListItemSupport
import com.fake.soundremote.ui.util.NavigateUpButton
import java.io.Serializable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KeystrokeListScreen(
    state: KeystrokeListUIState,
    onCreate: () -> Unit,
    onEdit: (id: Int) -> Unit,
    onDelete: (id: Int) -> Unit,
    onChangeFavoured: (id: Int, favoured: Boolean) -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        TopAppBar(
            title = { Text(stringResource(R.string.keystroke_list_title)) },
            navigationIcon = { NavigateUpButton(onNavigateUp) },
            actions = {
                IconButton(
                    onClick = onCreate
                ) {
                    Icon(Icons.Default.Add, stringResource(R.string.action_keystroke_create))
                }
            }
        )
        KeystrokeList(
            keystrokes = state.keystrokes,
            onChangeFavoured = { id, fav -> onChangeFavoured(id, fav) },
            onEdit = onEdit,
            onMove = { from, to -> onMove(from, to) },
            onDelete = { onDelete(it) },
        )
    }
}

private data class VisibleItemInfo(var index: Int, var offset: Int)
private data class DeleteInfo(val id: Int, val name: String) : Serializable

@Composable
private fun KeystrokeList(
    keystrokes: List<KeystrokeUIState>,
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
    val listDragState =
        rememberListDragState(
            key = keystrokes,
            onMove = onMove,
            listState = listState,
            onFirstItemChange = { firstVisibleItem = it },
        )
    LaunchedEffect(keystrokes) {
        firstVisibleItem?.let {
            listState.scrollToItem(it.index, it.offset)
            firstVisibleItem = null
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxHeight(),
        state = listState,
    ) {
        itemsIndexed(
            items = keystrokes,
            key = { _, keystroke -> keystroke.id },
        ) { index, keystrokeState ->
            KeystrokeItem(
                name = keystrokeState.name,
                description = keystrokeState.description,
                favoured = keystrokeState.favoured,
                onChangeFavoured = { onChangeFavoured(keystrokeState.id, it) },
                onEdit = { onEdit(keystrokeState.id) },
                onDelete = { toDelete = DeleteInfo(keystrokeState.id, keystrokeState.name) },
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
            text = { Text(stringResource(R.string.keystroke_delete_confirmation).format(name)) }
        )
    }
}

private enum class DragState { DRAGGED, SHIFTED, DEFAULT }
private data class DragInfo(val state: DragState = DragState.DEFAULT, val offset: Float = 0f)

@Composable
private fun KeystrokeItem(
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
        label = "Keystroke item drag"
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
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                ListItemHeadline(text = name)
                ListItemSupport(text = description)
            }
            Icon(
                Icons.Default.Menu,
                contentDescription = stringResource(R.string.drag_handle_description),
                modifier = Modifier.draggable(
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
                        Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.keystroke_actions_menu_description)
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

@Composable
private fun rememberListDragState(
    key: Any?,
    onMove: (from: Int, to: Int) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    onFirstItemChange: (VisibleItemInfo) -> Unit,
): ListDragState {
    return remember(key) {
        ListDragState(
            listState = listState,
            onMove = onMove,
            onFirstItemChange = onFirstItemChange
        )
    }
}

private class ListDragState(
    private val listState: LazyListState,
    private val onMove: (from: Int, to: Int) -> Unit,
    private val onFirstItemChange: (VisibleItemInfo) -> Unit,
) {
    private val itemsInfo by derivedStateOf { listState.layoutInfo.visibleItemsInfo }

    private var draggedItemInfo: LazyListItemInfo? by mutableStateOf(null)
    private var draggedDistance: Float by mutableFloatStateOf(0f)

    // Items that are currently shifted by the dragged item.
    private val shiftedItemsIndices by derivedStateOf {
        val draggedIndex = draggedItemInfo?.index ?: return@derivedStateOf IntRange.EMPTY
        val draggedOffsetTotal = draggedDistance + draggedItemInfo!!.offset
        if (draggedDistance > 0) {
            var currentItemVisibleIndex = itemsInfo.lastIndex
            while (
                currentItemVisibleIndex > 0 &&
                itemsInfo[currentItemVisibleIndex].index > draggedIndex &&
                itemsInfo[currentItemVisibleIndex].offset > draggedOffsetTotal
            ) {
                currentItemVisibleIndex--
            }
            (draggedIndex + 1)..itemsInfo[currentItemVisibleIndex].index
        } else {
            var currentItemVisibleIndex = 0
            while (
                currentItemVisibleIndex < itemsInfo.lastIndex &&
                itemsInfo[currentItemVisibleIndex].index < draggedIndex &&
                itemsInfo[currentItemVisibleIndex].offset < draggedOffsetTotal
            ) {
                currentItemVisibleIndex++
            }
            itemsInfo[currentItemVisibleIndex].index until draggedIndex
        }
    }
    private val offsetSign by derivedStateOf { if (draggedDistance > 0) -1 else 1 }

    val isDragActive: Boolean
        get() = draggedItemInfo != null

    fun onDragStart(draggedItemAbsoluteIndex: Int) {
        draggedItemInfo = itemsInfo[draggedItemAbsoluteIndex - listState.firstVisibleItemIndex]
    }

    fun onDrag(delta: Float) {
        draggedDistance += delta
    }

    fun onDragStop() {
        if (shiftedItemsIndices.isEmpty()) {
            draggedItemInfo = null
            draggedDistance = 0f
        } else {
            val fromIndex = draggedItemInfo!!.index
            val toIndex =
                if (offsetSign < 0) shiftedItemsIndices.last else shiftedItemsIndices.first
            val firstItemOffset = listState.firstVisibleItemScrollOffset
            when (listState.firstVisibleItemIndex) {
                fromIndex -> onFirstItemChange(VisibleItemInfo(fromIndex, firstItemOffset))
                toIndex -> onFirstItemChange(VisibleItemInfo(toIndex, firstItemOffset))
            }
            onMove(fromIndex, toIndex)
        }
    }

    fun dragInfo(index: Int): DragInfo {
        val draggedItem = draggedItemInfo ?: return DragInfo()
        return when (index) {
            draggedItem.index -> DragInfo(DragState.DRAGGED, draggedDistance)
            in shiftedItemsIndices -> DragInfo(
                DragState.SHIFTED,
                (draggedItem.size * offsetSign).toFloat()
            )

            else -> DragInfo()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CheckedItemPreview() {
    KeystrokeItem(
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
    KeystrokeItem(
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