package com.bitwarden.authenticator.ui.platform.manager.di

import com.bitwarden.authenticator.ui.platform.manager.AuthenticatorBuildInfoManagerImpl
import com.bitwarden.authenticator.ui.platform.model.SnackbarRelay
import com.bitwarden.core.data.manager.BuildInfoManager
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides UI-based managers in the platform package.
 */
@Module
@InstallIn(SingletonComponent::class)
class PlatformUiManagerModule {
    @Provides
    @Singleton
    fun provideBuildInfoManager(): BuildInfoManager = AuthenticatorBuildInfoManagerImpl()

    @Provides
    @Singleton
    fun provideSnackbarRelayManager(
        dispatcherManager: DispatcherManager,
    ): SnackbarRelayManager<SnackbarRelay> = SnackbarRelayManagerImpl(
        dispatcherManager = dispatcherManager,
    )
}
