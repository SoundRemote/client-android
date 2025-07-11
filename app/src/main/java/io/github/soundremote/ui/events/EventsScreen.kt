@file:OptIn(ExperimentalPermissionsApi::class)

package io.github.soundremote.ui.events

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import io.github.soundremote.R
import io.github.soundremote.data.Action
import io.github.soundremote.data.ActionType
import io.github.soundremote.data.Event
import io.github.soundremote.ui.components.ActionSelectDialog
import io.github.soundremote.ui.components.ListItemHeadline
import io.github.soundremote.ui.components.ListItemSupport
import io.github.soundremote.ui.components.NavigateUpButton
import io.github.soundremote.util.TextValue
import io.github.soundremote.util.showAppInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun EventsScreen(
    eventsUIState: EventsUIState,
    onSetActionForEvent: (eventId: Int, action: Action?) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showActionSelect by rememberSaveable { mutableStateOf(false) }
    var selectedEventId: Int? by rememberSaveable { mutableStateOf(null) }
    var selectedAction: Action? by remember { mutableStateOf(null) }

    val permissionStates = mutableMapOf<String, PermissionState>()
    for (event in eventsUIState.events) {
        event.permission?.also { permission ->
            if (!permissionStates.containsKey(permission.id)) {
                permissionStates[permission.id] = rememberPermissionState(permission.id)
            }
        }
    }

    fun checkAndRequestPermission(eventId: Int, action: Action?) {
        val permission = eventsUIState.events.find { it.id == eventId }?.permission
        if (action == null || permission == null) return
        val permissionState = permissionStates[permission.id]!!
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }

    fun onSelectAction(action: Action?) {
        val eventId = selectedEventId ?: return
        checkAndRequestPermission(eventId, action)
        if (action == null) {
            onSetActionForEvent(eventId, null)
        } else {
            onSetActionForEvent(eventId, action)
        }
    }

    Events(
        events = eventsUIState.events,
        permissionStates = permissionStates,
        onEventClick = { eventId, action ->
            selectedEventId = eventId
            selectedAction = action
            showActionSelect = true
        },
        onNavigateUp = onNavigateUp,
        modifier = modifier,
    )
    if (showActionSelect) {
        val actionTypes = Event.getById(selectedEventId!!).applicableActionTypes
        ActionSelectDialog(
            availableActionTypes = actionTypes,
            onConfirm = { action ->
                onSelectAction(action)
                showActionSelect = false
            },
            initialAction = selectedAction,
            onDismiss = { showActionSelect = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Events(
    events: List<EventUIState>,
    permissionStates: Map<String, PermissionState>,
    onEventClick: (Int, Action?) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
        TopAppBar(
            title = { Text(stringResource(R.string.event_list_title)) },
            navigationIcon = { NavigateUpButton(onNavigateUp) },
            scrollBehavior = scrollBehavior,
        )
        Column(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
        ) {
            for (event in events) {
                EventItem(
                    eventName = stringResource(event.nameStringId),
                    actionName = event.action?.let { action ->
                        val actionTypeName = stringResource(action.type.nameStringId)
                        val actionName = when (action.name) {
                            is TextValue.TextResource -> stringResource(action.name.strId)
                            is TextValue.TextString -> action.name.str
                        }
                        "$actionTypeName: $actionName"
                    },
                    permissionNameId = event.permission?.nameStringId,
                    permissionState = permissionStates[event.permission?.id],
                    onClick = {
                        val action = event.action?.let { Action(it.type, it.id) }
                        onEventClick(event.id, action)
                    }
                )
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.safeDrawing))
        }
    }
}

private val eventItemModifier = Modifier
    .height(72.dp)
    .padding(horizontal = 16.dp, vertical = 8.dp)

@Composable
private fun EventItem(
    eventName: String,
    actionName: String?,
    permissionNameId: Int?,
    permissionState: PermissionState?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column {
        Row(
            modifier = modifier
                .clickable(onClick = onClick)
                .then(eventItemModifier),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f),
            ) {
                ListItemHeadline(text = eventName)
                ListItemSupport(text = actionName ?: stringResource(R.string.action_none))
            }
            if (permissionState != null && permissionNameId != null) {
                PermissionInfo(permissionState, permissionNameId)
            }
        }
        HorizontalDivider()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionInfo(
    permissionState: PermissionState,
    permissionNameStringId: Int,
) {
    val scope = rememberCoroutineScope()
    val tooltipState = rememberTooltipState(isPersistent = true)

    val permissionText = if (permissionState.status.isGranted) {
        stringResource(R.string.permission_granted_caption)
    } else if (permissionState.status.shouldShowRationale) {
        // If the user has denied the permission but the rationale can be shown.
        stringResource(R.string.permission_required_caption)
    } else {
        // If it's the first time the user lands on this feature, or the user
        // doesn't want to be asked again for this permission,
        stringResource(R.string.permission_denied_caption)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.permission_caption),
                style = typography.labelSmall,
            )
            Text(
                text = permissionText,
                style = typography.labelSmall,
            )
        }
        TooltipBox(
            positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
            tooltip = {
                RichTooltip(
                    title = {
                        Text(stringResource(R.string.permission_tooltip_title))
                    },
                    action = {
                        val context = LocalContext.current
                        TextButton(
                            onClick = {
                                showAppInfo(context)
                                scope.launch {
                                    tooltipState.dismiss()
                                }
                            }
                        ) {
                            Text(stringResource(R.string.app_info))
                        }
                    }
                ) {
                    Text(
                        stringResource(R.string.permission_tooltip_text_template)
                            .format(stringResource(permissionNameStringId))
                    )
                }
            },
            state = tooltipState,
        ) {
            IconButton(
                onClick = { scope.launch { tooltipState.show() } },
            ) {
                Icon(Icons.Default.Info, stringResource(R.string.permission_show_info_caption))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EventItemPreview() {
    EventItem(
        eventName = "Event name",
        actionName = "Action name",
        permissionState = null,
        permissionNameId = null,
        onClick = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun EventsPreview() {
    Events(
        events = Event.entries.map { event ->
            EventUIState(
                id = event.id,
                nameStringId = event.nameStringId,
                permission = event.requiredPermission,
                action = ActionUIState(
                    ActionType.HOTKEY,
                    42,
                    TextValue.TextString("Ctrl + Shift + ${event.id}")
                ),
                applicableActionTypes = emptySet(),
            )
        },
        permissionStates = emptyMap(),
        onEventClick = { _, _ -> },
        onNavigateUp = {},
    )
}
