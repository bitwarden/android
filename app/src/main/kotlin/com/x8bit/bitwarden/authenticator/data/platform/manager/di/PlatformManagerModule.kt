package com.x8bit.bitwarden.authenticator.data.platform.manager.di

import com.x8bit.bitwarden.authenticator.data.platform.manager.DispatcherManager
import com.x8bit.bitwarden.authenticator.data.platform.manager.DispatcherManagerImpl
import com.x8bit.bitwarden.authenticator.data.platform.manager.SdkClientManager
import com.x8bit.bitwarden.authenticator.data.platform.manager.SdkClientManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * Provides managers in the platform package.
 */
@Module
@InstallIn(SingletonComponent::class)
object PlatformManagerModule {

    @Provides
    @Singleton
    fun provideBitwardenDispatchers(): DispatcherManager = DispatcherManagerImpl()

    @Provides
    @Singleton
    fun provideSdkClientManager(): SdkClientManager = SdkClientManagerImpl()

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
