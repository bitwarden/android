package com.x8bit.bitwarden.ui.platform.manager.di

import android.content.Context
import com.bitwarden.core.data.manager.BuildInfoManager
import com.bitwarden.data.manager.DispatcherManager
import com.x8bit.bitwarden.ui.platform.manager.BitwardenBuildInfoManagerImpl
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManagerImpl
import com.x8bit.bitwarden.ui.platform.manager.resource.ResourceManager
import com.x8bit.bitwarden.ui.platform.manager.resource.ResourceManagerImpl
import com.x8bit.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.x8bit.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * Provides UI-based managers in the platform package.
 */
@Module
@InstallIn(SingletonComponent::class)
object PlatformUiManagerModule {

    @Provides
    @Singleton
    fun provideBuildInfoManager(): BuildInfoManager = BitwardenBuildInfoManagerImpl()

    @Provides
    @Singleton
    fun provideIntentManager(
        @ApplicationContext context: Context,
        clock: Clock,
    ): IntentManager =
        IntentManagerImpl(
            context = context,
            clock = clock,
        )

    @Provides
    @Singleton
    fun provideResourceManager(@ApplicationContext context: Context): ResourceManager =
        ResourceManagerImpl(context = context)

    @Provides
    @Singleton
    fun provideSnackbarRelayManager(
        dispatcherManager: DispatcherManager,
    ): SnackbarRelayManager = SnackbarRelayManagerImpl(
        dispatcherManager = dispatcherManager,
    )
}
