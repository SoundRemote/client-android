package io.github.soundremote.service

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ServiceModule {

    @Binds
    @Singleton
    fun bindsServiceRepository(
        serviceRepository: MainServiceRepository,
    ): ServiceRepository
}
