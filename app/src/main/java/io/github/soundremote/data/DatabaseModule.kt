package io.github.soundremote.data

import android.content.Context
import androidx.room.Room.databaseBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.soundremote.data.room.AppDatabase
import io.github.soundremote.data.room.EventActionDao
import io.github.soundremote.data.room.HotkeyDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
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
