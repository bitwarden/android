package com.x8bit.bitwarden.data.vault.repository.di

import com.bitwarden.data.manager.DispatcherManager
import com.bitwarden.network.service.CiphersService
import com.bitwarden.network.service.FolderService
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.DatabaseSchemeManager
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.manager.CipherManager
import com.x8bit.bitwarden.data.vault.manager.CredentialExchangeImportManager
import com.x8bit.bitwarden.data.vault.manager.SendManager
import com.x8bit.bitwarden.data.vault.manager.TotpCodeManager
import com.x8bit.bitwarden.data.vault.manager.VaultLockManager
import com.x8bit.bitwarden.data.vault.manager.VaultSyncManager
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
        ciphersService: CiphersService,
        folderService: FolderService,
        vaultDiskSource: VaultDiskSource,
        vaultSdkSource: VaultSdkSource,
        authDiskSource: AuthDiskSource,
        settingsDiskSource: SettingsDiskSource,
        cipherManager: CipherManager,
        sendManager: SendManager,
        vaultLockManager: VaultLockManager,
        dispatcherManager: DispatcherManager,
        totpCodeManager: TotpCodeManager,
        pushManager: PushManager,
        databaseSchemeManager: DatabaseSchemeManager,
        clock: Clock,
        vaultSyncManager: VaultSyncManager,
        credentialExchangeImportManager: CredentialExchangeImportManager,
    ): VaultRepository = VaultRepositoryImpl(
        ciphersService = ciphersService,
        folderService = folderService,
        vaultDiskSource = vaultDiskSource,
        vaultSdkSource = vaultSdkSource,
        authDiskSource = authDiskSource,
        settingsDiskSource = settingsDiskSource,
        cipherManager = cipherManager,
        sendManager = sendManager,
        vaultLockManager = vaultLockManager,
        dispatcherManager = dispatcherManager,
        totpCodeManager = totpCodeManager,
        pushManager = pushManager,
        databaseSchemeManager = databaseSchemeManager,
        clock = clock,
        vaultSyncManager = vaultSyncManager,
        credentialExchangeImportManager = credentialExchangeImportManager,
    )
}
