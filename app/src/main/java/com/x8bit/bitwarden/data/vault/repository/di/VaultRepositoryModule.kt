package com.x8bit.bitwarden.data.vault.repository.di

import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.vault.datasource.network.service.SyncService
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.VaultRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides repositories in the vault package.
 */
@Module
@InstallIn(SingletonComponent::class)
class VaultRepositoryModule {

    @Provides
    @Singleton
    fun providesVaultRepository(
        syncService: SyncService,
        vaultSdkSource: VaultSdkSource,
        dispatcherManager: DispatcherManager,
    ): VaultRepository = VaultRepositoryImpl(
        syncService = syncService,
        vaultSdkSource = vaultSdkSource,
        dispatcherManager = dispatcherManager,
    )
}
