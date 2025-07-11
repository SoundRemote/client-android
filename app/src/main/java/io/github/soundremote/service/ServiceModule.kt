package io.github.soundremote.service

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface ServiceModule {

    @Binds
    fun bindsServiceManager(
        serviceManager: MainServiceManager,
    ): ServiceManager
}
