package com.fake.soundremote.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fake.soundremote.data.room.AppDatabase
import kotlinx.coroutines.CoroutineDispatcher
import org.junit.rules.ExternalResource

internal class DatabaseResource(private val dispatcher: CoroutineDispatcher) : ExternalResource() {
    private lateinit var db: AppDatabase
    lateinit var eventActionRepository: SystemEventActionRepository
        private set
    lateinit var hotkeyRepository: UserHotkeyRepository
        private set

    override fun before() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(AppDatabase.Callback())
            .build()
        eventActionRepository = SystemEventActionRepository(db.eventActionDao(), dispatcher)
        hotkeyRepository = UserHotkeyRepository(db.hotkeyDao(), dispatcher)
    }

    override fun after() {
        db.close()
    }
}
