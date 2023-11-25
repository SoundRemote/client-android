package com.fake.soundremote.data

import com.fake.soundremote.data.room.EventActionDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemEventActionRepository(
    private val eventActionDao: EventActionDao,
    private val dispatcher: CoroutineDispatcher,
) : EventActionRepository {
    @Inject
    constructor(eventActionDao: EventActionDao) : this(eventActionDao, Dispatchers.IO)

    override suspend fun getById(id: Int): EventAction? = withContext(dispatcher) {
        eventActionDao.getById(id)
    }

    override suspend fun insert(eventAction: EventAction) = withContext(dispatcher) {
        eventActionDao.insert(eventAction)
    }

    override suspend fun update(eventAction: EventAction) = withContext(dispatcher) {
        eventActionDao.update(eventAction)
    }

    override suspend fun deleteById(id: Int) = withContext(dispatcher) {
        eventActionDao.deleteById(id)
    }

    override suspend fun getKeystrokeByEventId(id: Int): Keystroke? =
        eventActionDao.getKeystrokeByEventId(id)

    override fun getAll(): Flow<List<EventAction>> =
        eventActionDao.getAll()
}
