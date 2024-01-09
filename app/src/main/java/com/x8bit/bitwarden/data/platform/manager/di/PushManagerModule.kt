package com.x8bit.bitwarden.data.platform.manager.di

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.PushDiskSource
import com.x8bit.bitwarden.data.platform.datasource.network.service.PushService
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.platform.manager.PushManagerImpl
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * Provides repositories in the push package.
 */
@Module
@InstallIn(SingletonComponent::class)
object PushManagerModule {

    @Provides
    @Singleton
    fun providePushManager(
        authDiskSource: AuthDiskSource,
        pushDiskSource: PushDiskSource,
        pushService: PushService,
        dispatcherManager: DispatcherManager,
        clock: Clock,
    ): PushManager = PushManagerImpl(
        authDiskSource = authDiskSource,
        pushDiskSource = pushDiskSource,
        pushService = pushService,
        dispatcherManager = dispatcherManager,
        clock = clock,
    )
}
