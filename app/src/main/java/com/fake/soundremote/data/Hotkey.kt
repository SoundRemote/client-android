package com.fake.soundremote.data

import android.provider.BaseColumns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.fake.soundremote.util.KeyCode
import com.fake.soundremote.util.Mods
import com.fake.soundremote.util.generateDescription

@Entity(tableName = Hotkey.TABLE_NAME)
data class Hotkey(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = COLUMN_ID)
    var id: Int = 0,
    @ColumnInfo(name = COLUMN_KEY_CODE)
    var keyCode: KeyCode,
    @ColumnInfo(name = COLUMN_MODS)
    var mods: Mods,
    @ColumnInfo(name = COLUMN_NAME)
    var name: String,
    @ColumnInfo(name = COLUMN_FAVOURED)
    var isFavoured: Boolean,
    // Hotkeys are ordered by this number descending, so new items with order value of 0 will
    // appear below.
    @ColumnInfo(name = COLUMN_ORDER, defaultValue = "$ORDER_DEFAULT_VALUE")
    var order: Int,
) {
    /**
     * Creates [Hotkey]
     *
     * @param keyCode  Windows Virtual-Key code of the main key
     * @param name     text description
     * @param mods     modifier keys
     * @param favoured should the hotkey be visible on the home screen
     */
    @Ignore
    constructor(keyCode: KeyCode, name: String, mods: Mods? = null, favoured: Boolean = true) :
            this(
                keyCode = keyCode,
                name = name,
                mods = mods ?: Mods(0),
                isFavoured = favoured,
                order = ORDER_DEFAULT_VALUE,
            )

    override fun toString(): String {
        val isFav = if (isFavoured) "Yes" else "No"
        return "${generateDescription(this)} (Title: $name, favoured: $isFav)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Hotkey
        if (id != other.id) return false
        if (keyCode != other.keyCode) return false
        if (mods != other.mods) return false
        if (name != other.name) return false
        return isFavoured == other.isFavoured
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + keyCode.value
        result = 31 * result + mods.value
        result = 31 * result + name.hashCode()
        result = 31 * result + isFavoured.hashCode()
        result = 31 * result + order
        return result
    }

    companion object {
        const val TABLE_NAME = "hotkey"
        const val COLUMN_ID = BaseColumns._ID
        const val COLUMN_KEY_CODE = "key_code"
        const val COLUMN_MODS = "mods"
        const val COLUMN_NAME = "name"
        const val COLUMN_FAVOURED = "favoured"
        const val COLUMN_ORDER = "display_order"
        const val ORDER_DEFAULT_VALUE = 0
    }
}

data class HotkeyInfo(
    @ColumnInfo(name = Hotkey.COLUMN_ID)
    var id: Int,
    @ColumnInfo(name = Hotkey.COLUMN_KEY_CODE)
    var keyCode: KeyCode,
    @ColumnInfo(name = Hotkey.COLUMN_MODS)
    var mods: Mods,
    @ColumnInfo(name = Hotkey.COLUMN_NAME)
    var name: String,
)

data class HotkeyOrder(
    @ColumnInfo(name = Hotkey.COLUMN_ID)
    var id: Int,
    @ColumnInfo(name = Hotkey.COLUMN_ORDER)
    var order: Int,
)
