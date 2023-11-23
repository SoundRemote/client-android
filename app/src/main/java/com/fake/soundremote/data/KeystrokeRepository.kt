package com.fake.soundremote.data

import kotlinx.coroutines.flow.Flow

interface KeystrokeRepository {
    suspend fun getById(id: Int): Keystroke?

    suspend fun insert(keystroke: Keystroke): Long

    suspend fun update(keystroke: Keystroke): Int

    suspend fun delete(keystroke: Keystroke): Int

    suspend fun deleteById(id: Int)

    suspend fun changeFavoured(id: Int, favoured: Boolean)

    suspend fun getAllOrderedOneshot(): List<Keystroke>

    fun getFavouredOrdered(favoured: Boolean): Flow<List<KeystrokeInfo>>

    fun getAllOrdered(): Flow<List<Keystroke>>

    suspend fun getAllOrdersOneshot(): List<KeystrokeOrder>

    suspend fun updateOrders(keystrokeOrders: List<KeystrokeOrder>)
}
