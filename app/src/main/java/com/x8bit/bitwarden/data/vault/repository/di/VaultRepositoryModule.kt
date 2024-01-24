package com.x8bit.bitwarden.data.vault.repository.di

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.network.service.CiphersService
import com.x8bit.bitwarden.data.vault.datasource.network.service.SendsService
import com.x8bit.bitwarden.data.vault.datasource.network.service.SyncService
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.manager.FileManager
import com.x8bit.bitwarden.data.vault.manager.TotpCodeManager
import com.x8bit.bitwarden.data.vault.manager.VaultLockManager
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.VaultRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * Provides repositories in the vault package.
 */
@Module
@InstallIn(SingletonComponent::class)
object VaultRepositoryModule {

    @Provides
    @Singleton
    fun providesVaultRepository(
        syncService: SyncService,
        sendsService: SendsService,
        ciphersService: CiphersService,
        vaultDiskSource: VaultDiskSource,
        vaultSdkSource: VaultSdkSource,
        authDiskSource: AuthDiskSource,
        settingsDiskSource: SettingsDiskSource,
        fileManager: FileManager,
        vaultLockManager: VaultLockManager,
        dispatcherManager: DispatcherManager,
        totpCodeManager: TotpCodeManager,
        clock: Clock,
    ): VaultRepository = VaultRepositoryImpl(
        syncService = syncService,
        sendsService = sendsService,
        ciphersService = ciphersService,
        vaultDiskSource = vaultDiskSource,
        vaultSdkSource = vaultSdkSource,
        authDiskSource = authDiskSource,
        settingsDiskSource = settingsDiskSource,
        fileManager = fileManager,
        vaultLockManager = vaultLockManager,
        dispatcherManager = dispatcherManager,
        totpCodeManager = totpCodeManager,
        clock = clock,
    )
}
