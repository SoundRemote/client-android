package com.fake.soundremote.data

import androidx.annotation.StringRes
import com.fake.soundremote.R
import com.fake.soundremote.util.AppPermission

internal enum class Event(
    val id: Int,
    @StringRes
    val nameStringId: Int,
    val applicableActionTypes: Set<ActionType>,
    val requiredPermission: AppPermission?,
    // Min SDK version for permission to be requested for the event.
    // If == null permission will always be requested.
    val permissionMinSdk: Int?,
) {
    CALL_BEGIN(
        1,
        R.string.event_name_call_start,
        setOf(ActionType.HOTKEY),
        AppPermission.Phone,
        31,
    ),
    CALL_END(
        2,
        R.string.event_name_call_end,
        setOf(ActionType.HOTKEY),
        AppPermission.Phone,
        31,
    ),
    SHAKE(
        100,
        R.string.event_name_shake,
        ActionType.entries.toSet(),
        null,
        null,
    );

    companion object {
        /**
         * Get [Event] by its id.
         * @throws [NoSuchElementException] if no entry with such id is found.
         */
        fun getById(id: Int): Event {
            return Event.entries.first { it.id == id }
        }
    }
}
