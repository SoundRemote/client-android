package io.github.soundremote.ui.hotkeylist

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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

sealed interface DragState {
    data object Dragged : DragState
    data class Shifted(val offset: Int) : DragState
    data object Default : DragState
}

@Stable
class ListDragState(
    private val listState: LazyListState,
    private val onMove: (from: Int, to: Int) -> Unit,
    private val onFirstVisibleItemChange: (VisibleItemInfo) -> Unit,
) {
    private var draggedItemInfo: LazyListItemInfo? by mutableStateOf(null)
    private var draggedDistance: Float by mutableFloatStateOf(0f)

    var draggedItemIndex by mutableIntStateOf(-1)
        private set

    var shiftedState: DragState by mutableStateOf(DragState.Shifted(0))
        private set

    // Items that are currently shifted by the dragged item.
    val shiftedItemsIndices by derivedStateOf {
        val draggedItem = draggedItemInfo ?: return@derivedStateOf IntRange.EMPTY
        val visibleItems = listState.layoutInfo.visibleItemsInfo
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

    fun onDragStart(draggedItemAbsoluteIndex: Int) {
        val draggedItemVisibleIndex = draggedItemAbsoluteIndex - listState.firstVisibleItemIndex
        draggedItemInfo = listState.layoutInfo.visibleItemsInfo[draggedItemVisibleIndex]
        draggedItemIndex = draggedItemAbsoluteIndex
    }

    fun onDrag(delta: Float) {
        draggedDistance += delta
        val draggedItemSize = draggedItemInfo?.size ?: return
        shiftedState = DragState.Shifted(
            if (draggedDistance > 0) -draggedItemSize else draggedItemSize
        )
    }

    fun onDragStop() {
        if (shiftedItemsIndices.isEmpty()) {
            draggedItemIndex = -1
            draggedItemInfo = null
            draggedDistance = 0f
        } else {
            val fromIndex = draggedItemInfo!!.index
            val toIndex =
                if (draggedDistance > 0) shiftedItemsIndices.last else shiftedItemsIndices.first
            val firstItemIndex = listState.firstVisibleItemIndex
            if (firstItemIndex == draggedItemIndex || firstItemIndex in shiftedItemsIndices) {
                val firstItemOffset = listState.firstVisibleItemScrollOffset
                onFirstVisibleItemChange(VisibleItemInfo(firstItemIndex, firstItemOffset))
            }
            onMove(fromIndex, toIndex)
        }
    }
}
