package com.fake.soundremote.data

import kotlinx.coroutines.flow.Flow

interface HotkeyRepository {
    suspend fun getById(id: Int): Hotkey?

    suspend fun insert(hotkey: Hotkey): Long

    suspend fun update(hotkey: Hotkey): Int

    suspend fun deleteById(id: Int)

    suspend fun changeFavoured(id: Int, favoured: Boolean)

    fun getFavouredOrdered(favoured: Boolean): Flow<List<HotkeyInfo>>

    fun getAllOrdered(): Flow<List<Hotkey>>

    fun getAllInfoOrdered(): Flow<List<HotkeyInfo>>

    suspend fun updateOrders(hotkeyOrders: List<HotkeyOrder>)
}
