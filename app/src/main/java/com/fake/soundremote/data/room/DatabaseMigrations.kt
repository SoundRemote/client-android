package com.fake.soundremote.data.room

import androidx.room.RenameColumn
import androidx.room.RenameTable
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    @RenameColumn(
        tableName = "event_action",
        fromColumnName = "keystroke_id",
        toColumnName = "action_id",
    )
    class Schema1to2 : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            // Set `action_type` field to `ActionType.KEYSTROKE.id` for all rows in `event_action`
            // table because keystroke was the only action type in the previous db version.
            db.execSQL("UPDATE event_action SET action_type = 2;")
        }
    }

    @RenameTable(
        fromTableName = "keystroke",
        toTableName = "hotkey",
    )
    class Schema2to3 : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            // Rename trigger
            db.execSQL("DROP TRIGGER IF EXISTS delete_event_action_on_keystroke_delete;")
            db.execSQL(CREATE_TRIGGER_DELETE_EVENT_ACTION_ON_HOTKEY_DELETE)
        }
    }
}
