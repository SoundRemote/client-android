package com.fake.soundremote.data

import com.fake.soundremote.data.room.HotkeyDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserHotkeyRepository(
    private val hotkeyDao: HotkeyDao,
    private val dispatcher: CoroutineDispatcher,
) : HotkeyRepository {
    @Inject
    constructor(hotkeyDao: HotkeyDao) :
            this(hotkeyDao, Dispatchers.IO)

    override suspend fun getById(id: Int): Hotkey? = withContext(dispatcher) {
        hotkeyDao.getById(id)
    }

    override suspend fun insert(hotkey: Hotkey) = withContext(dispatcher) {
        hotkeyDao.insert(hotkey)
    }

    override suspend fun update(hotkey: Hotkey) = withContext(dispatcher) {
        hotkeyDao.update(hotkey)
    }

    override suspend fun deleteById(id: Int) = withContext(dispatcher) {
        hotkeyDao.deleteById(id)
    }

    override suspend fun changeFavoured(id: Int, favoured: Boolean) = withContext(dispatcher) {
        hotkeyDao.changeFavoured(id, favoured)
    }

    override fun getFavouredOrdered(favoured: Boolean): Flow<List<HotkeyInfo>> =
        hotkeyDao.getFavouredOrdered(favoured)

    override fun getAllOrdered(): Flow<List<Hotkey>> =
        hotkeyDao.getAllOrdered()

    override fun getAllInfoOrdered(): Flow<List<HotkeyInfo>> =
        hotkeyDao.getAllInfoOrdered()

    override suspend fun updateOrders(hotkeyOrders: List<HotkeyOrder>) =
        withContext(dispatcher) {
            hotkeyDao.updateOrders(*hotkeyOrders.toTypedArray())
        }
}
