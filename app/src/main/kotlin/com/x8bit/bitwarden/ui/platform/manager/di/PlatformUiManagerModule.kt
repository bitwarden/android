package com.x8bit.bitwarden.ui.platform.manager.di

import android.content.Context
import com.bitwarden.core.data.manager.BuildInfoManager
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.ui.platform.manager.share.ShareManager
import com.bitwarden.ui.platform.manager.share.ShareManagerImpl
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManagerImpl
import com.x8bit.bitwarden.ui.platform.manager.BitwardenBuildInfoManagerImpl
import com.x8bit.bitwarden.ui.platform.manager.resource.ResourceManager
import com.x8bit.bitwarden.ui.platform.manager.resource.ResourceManagerImpl
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
    fun provideResourceManager(@ApplicationContext context: Context): ResourceManager =
        ResourceManagerImpl(context = context)

    @Provides
    @Singleton
    fun provideSnackbarRelayManager(
        dispatcherManager: DispatcherManager,
    ): SnackbarRelayManager<SnackbarRelay> = SnackbarRelayManagerImpl(
        dispatcherManager = dispatcherManager,
    )

    @Provides
    @Singleton
    fun provideShareManager(
        @ApplicationContext context: Context,
        buildInfoManager: BuildInfoManager,
    ): ShareManager = ShareManagerImpl(
        context = context,
        buildInfoManager = buildInfoManager,
    )
}
