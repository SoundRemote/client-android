package com.fake.soundremote.data

import androidx.annotation.StringRes
import com.fake.soundremote.R

enum class ActionType(
    val id: Int,
    @StringRes
    val nameStringId: Int,
) {
    APP(1, R.string.action_type_app),
    HOTKEY(2, R.string.action_type_hotkey);

    companion object {
        /**
         * Get [ActionType] by its id.
         * @throws [NoSuchElementException] if no entry with such id is found.
         */
        fun getById(id: Int): ActionType {
            return ActionType.entries.first { it.id == id }
        }
    }
}
