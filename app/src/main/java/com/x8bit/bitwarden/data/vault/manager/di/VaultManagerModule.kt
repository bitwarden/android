package com.x8bit.bitwarden.data.vault.manager.di

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.manager.VaultLockManager
import com.x8bit.bitwarden.data.vault.manager.VaultLockManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides managers in the vault package.
 */
@Module
@InstallIn(SingletonComponent::class)
object VaultManagerModule {

    @Provides
    @Singleton
    fun provideVaultLockManager(
        authDiskSource: AuthDiskSource,
        vaultSdkSource: VaultSdkSource,
        settingsRepository: SettingsRepository,
        dispatcherManager: DispatcherManager,
    ): VaultLockManager =
        VaultLockManagerImpl(
            authDiskSource = authDiskSource,
            vaultSdkSource = vaultSdkSource,
            settingsRepository = settingsRepository,
            dispatcherManager = dispatcherManager,
        )
}
