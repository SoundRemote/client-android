package com.fake.soundremote.ui.events

import android.os.Build
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fake.soundremote.data.ActionData
import com.fake.soundremote.data.Action
import com.fake.soundremote.data.ActionType
import com.fake.soundremote.data.AppAction
import com.fake.soundremote.data.Event
import com.fake.soundremote.data.EventAction
import com.fake.soundremote.data.EventActionRepository
import com.fake.soundremote.data.HotkeyRepository
import com.fake.soundremote.util.AppPermission
import com.fake.soundremote.util.TextValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class EventUIState(
    val id: Int,
    @StringRes
    val nameStringId: Int,
    val permission: AppPermission? = null,
    val action: ActionUIState? = null,
    val applicableActionTypes: Set<ActionType>,
)

internal data class ActionUIState(
    val type: ActionType,
    val id: Int,
    val name: TextValue,
)

internal data class EventsUIState(
    val events: List<EventUIState> = emptyList()
)

@HiltViewModel
internal class EventsViewModel @Inject constructor(
    private val eventActionRepository: EventActionRepository,
    private val hotkeyRepository: HotkeyRepository,
) : ViewModel() {
    val uiState: StateFlow<EventsUIState> = combine(
        flowOf(Event.entries.toTypedArray()),
        eventActionRepository.getAll(),
    ) { events, eventActions ->
        val eventUIStates = mutableListOf<EventUIState>()
        for (event in events) {
            val eventAction = eventActions.find { it.eventId == event.id }
            val actionUIState = eventAction?.action?.let { action ->
                val type = ActionType.getById(action.actionType)
                val name: TextValue = when (type) {
                    ActionType.APP -> {
                        TextValue.TextResource(AppAction.getById(action.actionId).nameStringId)
                    }

                    ActionType.HOTKEY -> {
                        TextValue.TextString(hotkeyRepository.getById(action.actionId)!!.name)
                    }
                }
                ActionUIState(type, action.actionId, name)
            }
            val permission = if (
                (event.permissionMinSdk == null) ||
                (event.permissionMinSdk <= Build.VERSION.SDK_INT)
            ) {
                event.requiredPermission
            } else {
                null
            }
            eventUIStates.add(
                EventUIState(
                    id = event.id,
                    nameStringId = event.nameStringId,
                    permission = permission,
                    action = actionUIState,
                    applicableActionTypes = event.applicableActionTypes,
                )
            )
        }
        EventsUIState(eventUIStates)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EventsUIState()
    )

    fun setActionForEvent(eventId: Int, action: Action?) {
        viewModelScope.launch {
            if (action == null) {
                eventActionRepository.deleteById(eventId)
            } else {
                val actionData = ActionData(action.type.id, action.id)
                val event = eventActionRepository.getById(eventId)
                if (event == null) {
                    eventActionRepository.insert(EventAction(eventId, actionData))
                } else {
                    event.action = actionData
                    eventActionRepository.update(event)
                }
            }
        }
    }
}
