package com.fake.soundremote.data.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import com.fake.soundremote.data.Hotkey
import com.fake.soundremote.data.HotkeyInfo
import com.fake.soundremote.data.HotkeyOrder
import kotlinx.coroutines.flow.Flow

@Dao
interface HotkeyDao : BaseDao<Hotkey> {
    @Query("SELECT * FROM ${Hotkey.TABLE_NAME} WHERE ${Hotkey.COLUMN_ID} = :id")
    suspend fun getById(id: Int): Hotkey?

    @Query("DELETE FROM ${Hotkey.TABLE_NAME} WHERE ${Hotkey.COLUMN_ID} = :id")
    suspend fun deleteById(id: Int)

    @Query(
        """
            UPDATE ${Hotkey.TABLE_NAME}
            SET ${Hotkey.COLUMN_FAVOURED} = :favoured
            WHERE ${Hotkey.COLUMN_ID} = :id;
        """
    )
    suspend fun changeFavoured(id: Int, favoured: Boolean)

    @Query(
        """
        SELECT
        ${Hotkey.COLUMN_ID},
        ${Hotkey.COLUMN_KEY_CODE},
        ${Hotkey.COLUMN_MODS},
        ${Hotkey.COLUMN_NAME}
        FROM ${Hotkey.TABLE_NAME}
        WHERE ${Hotkey.COLUMN_FAVOURED} = :favoured
        ORDER BY ${Hotkey.COLUMN_ORDER} DESC, ${Hotkey.COLUMN_ID};
        """
    )
    fun getFavouredOrdered(favoured: Boolean): Flow<List<HotkeyInfo>>

    @Query(
        """
        SELECT * FROM ${Hotkey.TABLE_NAME}
        ORDER BY ${Hotkey.COLUMN_ORDER} DESC, ${Hotkey.COLUMN_ID};
        """
    )
    fun getAllOrdered(): Flow<List<Hotkey>>

    @Query(
        """
        SELECT ${Hotkey.COLUMN_ID},
        ${Hotkey.COLUMN_KEY_CODE},
        ${Hotkey.COLUMN_MODS},
        ${Hotkey.COLUMN_NAME}
        FROM ${Hotkey.TABLE_NAME}
        ORDER BY ${Hotkey.COLUMN_ORDER} DESC, ${Hotkey.COLUMN_ID};
        """
    )
    fun getAllInfoOrdered(): Flow<List<HotkeyInfo>>

    @Update(entity = Hotkey::class)
    suspend fun updateOrders(vararg hotkeyOrders: HotkeyOrder)
}
