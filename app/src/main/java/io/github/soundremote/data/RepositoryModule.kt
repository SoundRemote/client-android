package io.github.soundremote.data

import io.github.soundremote.data.preferences.PreferencesRepository
import io.github.soundremote.data.preferences.UserPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
interface RepositoryModule {
    @Binds
    fun bindsPreferencesRepository(
        preferencesRepository: UserPreferencesRepository?,
    ): PreferencesRepository?

    @Binds
    fun bindsHotkeyRepository(
        hotkeyRepository: UserHotkeyRepository?,
    ): HotkeyRepository?

    @Binds
    fun bindsEventActionRepository(
        eventActionRepository: SystemEventActionRepository?,
    ): EventActionRepository?
}
