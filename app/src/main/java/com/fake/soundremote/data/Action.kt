package com.fake.soundremote.data

import androidx.room.ColumnInfo

data class Action(
    @ColumnInfo(name = COLUMN_TYPE, defaultValue = "0")
    var actionType: Int,

    @ColumnInfo(name = COLUMN_ID)
    var actionId: Int,
) {
    companion object {
        const val COLUMN_TYPE = "action_type"
        const val COLUMN_ID = "action_id"
    }
}
