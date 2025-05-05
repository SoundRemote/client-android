package io.github.soundremote.ui.hotkeylist

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun rememberListDragState(
    key: Any?,
    onMove: (from: Int, to: Int) -> Unit,
    onFirstVisibleItemChange: (VisibleItemInfo) -> Unit,
    listState: LazyListState = rememberLazyListState(),
): ListDragState {
    return remember(key) {
        ListDragState(
            listState = listState,
            onMove = onMove,
            onFirstVisibleItemChange = onFirstVisibleItemChange,
        )
    }
}

enum class DragState { DRAGGED, SHIFTED, DEFAULT }

data class DragInfo(val state: DragState = DragState.DEFAULT, val offset: Float = 0f)

@Stable
class ListDragState(
    private val listState: LazyListState,
    private val onMove: (from: Int, to: Int) -> Unit,
    private val onFirstVisibleItemChange: (VisibleItemInfo) -> Unit,
) {
    private var draggedItemInfo: LazyListItemInfo? by mutableStateOf(null)
    private var draggedDistance: Float by mutableFloatStateOf(0f)

    // Items that are currently shifted by the dragged item.
    private val shiftedItemsIndices by derivedStateOf {
        val visibleItems = listState.layoutInfo.visibleItemsInfo
        val draggedItem = draggedItemInfo ?: return@derivedStateOf IntRange.EMPTY
        val draggedOffsetTotal = draggedItem.offset + draggedDistance
        if (draggedDistance > 0) {
            var currentVisibleItemIndex = visibleItems.lastIndex
            while (
                currentVisibleItemIndex > 0 &&
                visibleItems[currentVisibleItemIndex].index > draggedItem.index &&
                visibleItems[currentVisibleItemIndex].offset > draggedOffsetTotal
            ) {
                currentVisibleItemIndex--
            }
            (draggedItem.index + 1)..visibleItems[currentVisibleItemIndex].index
        } else {
            var currentItemVisibleIndex = 0
            while (
                currentItemVisibleIndex < visibleItems.lastIndex &&
                visibleItems[currentItemVisibleIndex].index < draggedItem.index &&
                visibleItems[currentItemVisibleIndex].offset < draggedOffsetTotal
            ) {
                currentItemVisibleIndex++
            }
            visibleItems[currentItemVisibleIndex].index until draggedItem.index
        }
    }
    private val offsetSign by derivedStateOf { if (draggedDistance > 0) -1 else 1 }

    val isDragActive: Boolean
        get() = draggedItemInfo != null

    fun onDragStart(draggedItemAbsoluteIndex: Int) {
        val draggedItemVisibleIndex = draggedItemAbsoluteIndex - listState.firstVisibleItemIndex
        draggedItemInfo = listState.layoutInfo.visibleItemsInfo[draggedItemVisibleIndex]
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
            val firstItemIndex = listState.firstVisibleItemIndex
            val firstItemOffset = listState.firstVisibleItemScrollOffset
            onFirstVisibleItemChange(VisibleItemInfo(firstItemIndex, firstItemOffset))
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
