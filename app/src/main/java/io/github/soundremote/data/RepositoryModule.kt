package io.github.soundremote.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.soundremote.data.preferences.PreferencesRepository
import io.github.soundremote.data.preferences.UserPreferencesRepository

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {

    @Binds
    fun bindsPreferencesRepository(
        preferencesRepository: UserPreferencesRepository,
    ): PreferencesRepository

    @Binds
    fun bindsHotkeyRepository(
        hotkeyRepository: UserHotkeyRepository,
    ): HotkeyRepository

    @Binds
    fun bindsEventActionRepository(
        eventActionRepository: SystemEventActionRepository,
    ): EventActionRepository
}
