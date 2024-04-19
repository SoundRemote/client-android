package com.fake.soundremote.data.room

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fake.soundremote.data.ActionData
import com.fake.soundremote.data.ActionType
import com.fake.soundremote.data.EventAction
import com.fake.soundremote.data.Hotkey

@Database(
    entities = [
        Hotkey::class,
        EventAction::class
    ],
    version = 3,
    autoMigrations = [
        AutoMigration(from = 1, to = 2, spec = DatabaseMigrations.Schema1to2::class),
        AutoMigration(from = 2, to = 3, spec = DatabaseMigrations.Schema2to3::class),
    ],
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hotkeyDao(): HotkeyDao
    abstract fun eventActionDao(): EventActionDao

    class Callback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            db.execSQL(CREATE_TRIGGER_DELETE_EVENT_ACTION_ON_HOTKEY_DELETE)
        }
    }
}

// When a hotkey is deleted also delete all the event actions with that hotkey
val CREATE_TRIGGER_DELETE_EVENT_ACTION_ON_HOTKEY_DELETE = """
    CREATE TRIGGER IF NOT EXISTS delete_event_action_on_hotkey_delete
    AFTER DELETE ON ${Hotkey.TABLE_NAME}
    BEGIN
        DELETE FROM ${EventAction.TABLE_NAME}
        WHERE ${ActionData.COLUMN_TYPE} = ${ActionType.HOTKEY.id}
        AND ${ActionData.COLUMN_ID} = OLD.${Hotkey.COLUMN_ID};
    END
    """.trimIndent()
