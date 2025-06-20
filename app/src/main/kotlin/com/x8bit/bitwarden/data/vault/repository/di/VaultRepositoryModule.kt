package com.x8bit.bitwarden.data.vault.repository.di

import com.bitwarden.data.manager.DispatcherManager
import com.bitwarden.network.service.CiphersService
import com.bitwarden.network.service.FolderService
import com.bitwarden.network.service.SendsService
import com.bitwarden.network.service.SyncService
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.manager.UserLogoutManager
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.DatabaseSchemeManager
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.platform.manager.ReviewPromptManager
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.manager.CipherManager
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
        folderService: FolderService,
        vaultDiskSource: VaultDiskSource,
        vaultSdkSource: VaultSdkSource,
        authDiskSource: AuthDiskSource,
        settingsDiskSource: SettingsDiskSource,
        cipherManager: CipherManager,
        fileManager: FileManager,
        vaultLockManager: VaultLockManager,
        dispatcherManager: DispatcherManager,
        totpCodeManager: TotpCodeManager,
        pushManager: PushManager,
        userLogoutManager: UserLogoutManager,
        databaseSchemeManager: DatabaseSchemeManager,
        clock: Clock,
        reviewPromptManager: ReviewPromptManager,
    ): VaultRepository = VaultRepositoryImpl(
        syncService = syncService,
        sendsService = sendsService,
        ciphersService = ciphersService,
        folderService = folderService,
        vaultDiskSource = vaultDiskSource,
        vaultSdkSource = vaultSdkSource,
        authDiskSource = authDiskSource,
        settingsDiskSource = settingsDiskSource,
        cipherManager = cipherManager,
        fileManager = fileManager,
        vaultLockManager = vaultLockManager,
        dispatcherManager = dispatcherManager,
        totpCodeManager = totpCodeManager,
        pushManager = pushManager,
        userLogoutManager = userLogoutManager,
        databaseSchemeManager = databaseSchemeManager,
        clock = clock,
        reviewPromptManager = reviewPromptManager,
    )
}
