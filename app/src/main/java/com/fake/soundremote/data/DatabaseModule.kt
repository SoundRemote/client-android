package com.fake.soundremote.data

import android.content.Context
import androidx.room.Room.databaseBuilder
import com.fake.soundremote.data.room.AppDatabase
import com.fake.soundremote.data.room.EventActionDao
import com.fake.soundremote.data.room.HotkeyDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return databaseBuilder(appContext, AppDatabase::class.java, "sound_remote")
            .addCallback(AppDatabase.Callback())
            .build()
    }

    @Provides
    @Singleton
    fun provideHotkeyDao(database: AppDatabase): HotkeyDao {
        return database.hotkeyDao()
    }

    @Provides
    @Singleton
    fun provideEventActionDao(database: AppDatabase): EventActionDao {
        return database.eventActionDao()
    }
}
