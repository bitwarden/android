package com.bitwarden.authenticator.data.platform.manager.di

import android.content.Context
import com.bitwarden.authenticator.data.platform.manager.DispatcherManager
import com.bitwarden.authenticator.data.platform.manager.DispatcherManagerImpl
import com.bitwarden.authenticator.data.platform.manager.SdkClientManager
import com.bitwarden.authenticator.data.platform.manager.SdkClientManagerImpl
import com.bitwarden.authenticator.data.platform.manager.clipboard.BitwardenClipboardManager
import com.bitwarden.authenticator.data.platform.manager.clipboard.BitwardenClipboardManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
    fun provideBitwardenClipboardManager(
        @ApplicationContext context: Context,
    ): BitwardenClipboardManager = BitwardenClipboardManagerImpl(context)

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
