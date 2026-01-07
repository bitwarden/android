package com.x8bit.bitwarden.data.vault.manager.di

import android.content.Context
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.manager.realtime.RealtimeManager
import com.bitwarden.data.manager.file.FileManager
import com.bitwarden.network.service.CiphersService
import com.bitwarden.network.service.FolderService
import com.bitwarden.network.service.SendsService
import com.bitwarden.network.service.SyncService
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.sdk.AuthSdkSource
import com.x8bit.bitwarden.data.auth.manager.KdfManager
import com.x8bit.bitwarden.data.auth.manager.TrustedDeviceManager
import com.x8bit.bitwarden.data.auth.manager.UserLogoutManager
import com.x8bit.bitwarden.data.auth.manager.UserStateManager
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.AppStateManager
import com.x8bit.bitwarden.data.platform.manager.DatabaseSchemeManager
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.platform.manager.ReviewPromptManager
import com.x8bit.bitwarden.data.platform.manager.network.NetworkConnectionManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.manager.CipherManager
import com.x8bit.bitwarden.data.vault.manager.CipherManagerImpl
import com.x8bit.bitwarden.data.vault.manager.CredentialExchangeImportManager
import com.x8bit.bitwarden.data.vault.manager.CredentialExchangeImportManagerImpl
import com.x8bit.bitwarden.data.vault.manager.FolderManager
import com.x8bit.bitwarden.data.vault.manager.FolderManagerImpl
import com.x8bit.bitwarden.data.vault.manager.PinProtectedUserKeyManager
import com.x8bit.bitwarden.data.vault.manager.PinProtectedUserKeyManagerImpl
import com.x8bit.bitwarden.data.vault.manager.SendManager
import com.x8bit.bitwarden.data.vault.manager.SendManagerImpl
import com.x8bit.bitwarden.data.vault.manager.TotpCodeManager
import com.x8bit.bitwarden.data.vault.manager.TotpCodeManagerImpl
import com.x8bit.bitwarden.data.vault.manager.VaultLockManager
import com.x8bit.bitwarden.data.vault.manager.VaultLockManagerImpl
import com.x8bit.bitwarden.data.vault.manager.VaultMigrationManager
import com.x8bit.bitwarden.data.vault.manager.VaultMigrationManagerImpl
import com.x8bit.bitwarden.data.vault.manager.VaultSyncManager
import com.x8bit.bitwarden.data.vault.manager.VaultSyncManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import java.time.Clock
import javax.inject.Singleton

/**
 * Provides managers in the vault package.
 */
@Module
@InstallIn(SingletonComponent::class)
object VaultManagerModule {

    @Provides
    @Singleton
    fun provideVaultMigrationManager(
        authDiskSource: AuthDiskSource,
        vaultDiskSource: VaultDiskSource,
        settingsDiskSource: SettingsDiskSource,
        vaultLockManager: VaultLockManager,
        policyManager: PolicyManager,
        featureFlagManager: FeatureFlagManager,
        connectionManager: NetworkConnectionManager,
        dispatcherManager: DispatcherManager,
    ): VaultMigrationManager = VaultMigrationManagerImpl(
        authDiskSource = authDiskSource,
        vaultDiskSource = vaultDiskSource,
        settingsDiskSource = settingsDiskSource,
        vaultLockManager = vaultLockManager,
        policyManager = policyManager,
        featureFlagManager = featureFlagManager,
        connectionManager = connectionManager,
        dispatcherManager = dispatcherManager,
    )

    @Provides
    @Singleton
    fun provideCipherManager(
        ciphersService: CiphersService,
        settingsDiskSource: SettingsDiskSource,
        vaultDiskSource: VaultDiskSource,
        vaultSdkSource: VaultSdkSource,
        authDiskSource: AuthDiskSource,
        fileManager: FileManager,
        clock: Clock,
        reviewPromptManager: ReviewPromptManager,
        dispatcherManager: DispatcherManager,
        pushManager: PushManager,
    ): CipherManager = CipherManagerImpl(
        fileManager = fileManager,
        settingsDiskSource = settingsDiskSource,
        authDiskSource = authDiskSource,
        ciphersService = ciphersService,
        vaultDiskSource = vaultDiskSource,
        vaultSdkSource = vaultSdkSource,
        clock = clock,
        reviewPromptManager = reviewPromptManager,
        dispatcherManager = dispatcherManager,
        pushManager = pushManager,
    )

    @Provides
    @Singleton
    fun provideFolderManager(
        folderService: FolderService,
        settingsDiskSource: SettingsDiskSource,
        vaultDiskSource: VaultDiskSource,
        vaultSdkSource: VaultSdkSource,
        authDiskSource: AuthDiskSource,
        dispatcherManager: DispatcherManager,
        pushManager: PushManager,
    ): FolderManager = FolderManagerImpl(
        authDiskSource = authDiskSource,
        settingsDiskSource = settingsDiskSource,
        folderService = folderService,
        vaultDiskSource = vaultDiskSource,
        vaultSdkSource = vaultSdkSource,
        dispatcherManager = dispatcherManager,
        pushManager = pushManager,
    )

    @Provides
    @Singleton
    fun provideSendManager(
        sendsService: SendsService,
        vaultDiskSource: VaultDiskSource,
        vaultSdkSource: VaultSdkSource,
        authDiskSource: AuthDiskSource,
        settingsDiskSource: SettingsDiskSource,
        fileManager: FileManager,
        reviewPromptManager: ReviewPromptManager,
        pushManager: PushManager,
        dispatcherManager: DispatcherManager,
    ): SendManager = SendManagerImpl(
        fileManager = fileManager,
        authDiskSource = authDiskSource,
        settingsDiskSource = settingsDiskSource,
        sendsService = sendsService,
        vaultDiskSource = vaultDiskSource,
        vaultSdkSource = vaultSdkSource,
        reviewPromptManager = reviewPromptManager,
        pushManager = pushManager,
        dispatcherManager = dispatcherManager,
    )

    @Provides
    @Singleton
    fun provideVaultLockManager(
        @ApplicationContext context: Context,
        clock: Clock,
        realtimeManager: RealtimeManager,
        authDiskSource: AuthDiskSource,
        authSdkSource: AuthSdkSource,
        vaultSdkSource: VaultSdkSource,
        settingsRepository: SettingsRepository,
        appStateManager: AppStateManager,
        userLogoutManager: UserLogoutManager,
        dispatcherManager: DispatcherManager,
        trustedDeviceManager: TrustedDeviceManager,
        kdfManager: KdfManager,
        pinProtectedUserKeyManager: PinProtectedUserKeyManager,
    ): VaultLockManager =
        VaultLockManagerImpl(
            context = context,
            clock = clock,
            realtimeManager = realtimeManager,
            authDiskSource = authDiskSource,
            authSdkSource = authSdkSource,
            vaultSdkSource = vaultSdkSource,
            settingsRepository = settingsRepository,
            appStateManager = appStateManager,
            userLogoutManager = userLogoutManager,
            dispatcherManager = dispatcherManager,
            trustedDeviceManager = trustedDeviceManager,
            kdfManager = kdfManager,
            pinProtectedUserKeyManager = pinProtectedUserKeyManager,
        )

    @Provides
    @Singleton
    fun providePinProtectedUserKeyManager(
        authDiskSource: AuthDiskSource,
        vaultSdkSource: VaultSdkSource,
    ): PinProtectedUserKeyManager =
        PinProtectedUserKeyManagerImpl(
            authDiskSource = authDiskSource,
            vaultSdkSource = vaultSdkSource,
        )

    @Provides
    @Singleton
    fun provideTotpManager(
        vaultSdkSource: VaultSdkSource,
        dispatcherManager: DispatcherManager,
        clock: Clock,
    ): TotpCodeManager =
        TotpCodeManagerImpl(
            vaultSdkSource = vaultSdkSource,
            dispatcherManager = dispatcherManager,
            clock = clock,
        )

    @Provides
    @Singleton
    fun provideVaultSyncManager(
        syncService: SyncService,
        settingsDiskSource: SettingsDiskSource,
        authDiskSource: AuthDiskSource,
        vaultDiskSource: VaultDiskSource,
        vaultSdkSource: VaultSdkSource,
        userLogoutManager: UserLogoutManager,
        userStateManager: UserStateManager,
        vaultLockManager: VaultLockManager,
        clock: Clock,
        databaseSchemeManager: DatabaseSchemeManager,
        pushManager: PushManager,
        dispatcherManager: DispatcherManager,
    ): VaultSyncManager = VaultSyncManagerImpl(
        syncService = syncService,
        settingsDiskSource = settingsDiskSource,
        authDiskSource = authDiskSource,
        vaultDiskSource = vaultDiskSource,
        vaultSdkSource = vaultSdkSource,
        userLogoutManager = userLogoutManager,
        userStateManager = userStateManager,
        vaultLockManager = vaultLockManager,
        clock = clock,
        databaseSchemeManager = databaseSchemeManager,
        pushManager = pushManager,
        dispatcherManager = dispatcherManager,
    )

    @Provides
    @Singleton
    fun provideCredentialExchangeImportManager(
        vaultSdkSource: VaultSdkSource,
        ciphersService: CiphersService,
        vaultSyncManager: VaultSyncManager,
        policyManager: PolicyManager,
        json: Json,
    ): CredentialExchangeImportManager = CredentialExchangeImportManagerImpl(
        vaultSdkSource = vaultSdkSource,
        ciphersService = ciphersService,
        vaultSyncManager = vaultSyncManager,
        policyManager = policyManager,
        json = json,
    )
}
