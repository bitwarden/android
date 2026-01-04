package com.bitwarden.authenticator.data.authenticator.repository.di

import com.bitwarden.authenticator.data.authenticator.datasource.disk.AuthenticatorDiskSource
import com.bitwarden.authenticator.data.authenticator.manager.FileManager
import com.bitwarden.authenticator.data.authenticator.manager.TotpCodeManager
import com.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepositoryImpl
import com.bitwarden.authenticator.data.platform.manager.imports.ImportManager
import com.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.bitwarden.authenticatorbridge.manager.AuthenticatorBridgeManager
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides repositories in the authenticator package.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthenticatorRepositoryModule {

    @Provides
    @Singleton
    fun provideAuthenticatorRepository(
        authenticatorBridgeManager: AuthenticatorBridgeManager,
        authenticatorDiskSource: AuthenticatorDiskSource,
        dispatcherManager: DispatcherManager,
        fileManager: FileManager,
        importManager: ImportManager,
        totpCodeManager: TotpCodeManager,
        settingsRepository: SettingsRepository,
    ): AuthenticatorRepository = AuthenticatorRepositoryImpl(
        authenticatorBridgeManager = authenticatorBridgeManager,
        authenticatorDiskSource = authenticatorDiskSource,
        dispatcherManager = dispatcherManager,
        fileManager = fileManager,
        importManager = importManager,
        totpCodeManager = totpCodeManager,
        settingRepository = settingsRepository,
    )
}
