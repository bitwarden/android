package com.x8bit.bitwarden.data.auth.repository.di

import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.data.datasource.disk.ConfigDiskSource
import com.bitwarden.network.service.AccountsService
import com.bitwarden.network.service.DevicesService
import com.bitwarden.network.service.HaveIBeenPwnedService
import com.bitwarden.network.service.IdentityService
import com.bitwarden.network.service.OrganizationService
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.sdk.AuthSdkSource
import com.x8bit.bitwarden.data.auth.manager.AuthRequestManager
import com.x8bit.bitwarden.data.auth.manager.KdfManager
import com.x8bit.bitwarden.data.auth.manager.KeyConnectorManager
import com.x8bit.bitwarden.data.auth.manager.TrustedDeviceManager
import com.x8bit.bitwarden.data.auth.manager.UserLogoutManager
import com.x8bit.bitwarden.data.auth.manager.UserStateManager
import com.x8bit.bitwarden.data.auth.manager.UserStateManagerImpl
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.AuthRepositoryImpl
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManager
import com.x8bit.bitwarden.data.platform.manager.LogsManager
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.manager.VaultLockManager
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * Provides repositories in the auth package.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthRepositoryModule {

    @Provides
    @Singleton
    fun providesAuthRepository(
        clock: Clock,
        accountsService: AccountsService,
        devicesService: DevicesService,
        identityService: IdentityService,
        haveIBeenPwnedService: HaveIBeenPwnedService,
        organizationService: OrganizationService,
        authSdkSource: AuthSdkSource,
        vaultSdkSource: VaultSdkSource,
        authDiskSource: AuthDiskSource,
        settingsDiskSource: SettingsDiskSource,
        configDiskSource: ConfigDiskSource,
        dispatcherManager: DispatcherManager,
        environmentRepository: EnvironmentRepository,
        settingsRepository: SettingsRepository,
        vaultRepository: VaultRepository,
        keyConnectorManager: KeyConnectorManager,
        authRequestManager: AuthRequestManager,
        trustedDeviceManager: TrustedDeviceManager,
        userLogoutManager: UserLogoutManager,
        pushManager: PushManager,
        policyManager: PolicyManager,
        logsManager: LogsManager,
        userStateManager: UserStateManager,
        kdfManager: KdfManager,
    ): AuthRepository = AuthRepositoryImpl(
        clock = clock,
        accountsService = accountsService,
        devicesService = devicesService,
        identityService = identityService,
        organizationService = organizationService,
        authSdkSource = authSdkSource,
        vaultSdkSource = vaultSdkSource,
        authDiskSource = authDiskSource,
        settingsDiskSource = settingsDiskSource,
        configDiskSource = configDiskSource,
        haveIBeenPwnedService = haveIBeenPwnedService,
        dispatcherManager = dispatcherManager,
        environmentRepository = environmentRepository,
        settingsRepository = settingsRepository,
        vaultRepository = vaultRepository,
        keyConnectorManager = keyConnectorManager,
        authRequestManager = authRequestManager,
        trustedDeviceManager = trustedDeviceManager,
        userLogoutManager = userLogoutManager,
        pushManager = pushManager,
        policyManager = policyManager,
        logsManager = logsManager,
        userStateManager = userStateManager,
        kdfManager = kdfManager,
    )

    @Provides
    @Singleton
    fun providesUserStateManager(
        authDiskSource: AuthDiskSource,
        firstTimeActionManager: FirstTimeActionManager,
        vaultLockManager: VaultLockManager,
        policyManager: PolicyManager,
        dispatcherManager: DispatcherManager,
    ): UserStateManager = UserStateManagerImpl(
        authDiskSource = authDiskSource,
        firstTimeActionManager = firstTimeActionManager,
        vaultLockManager = vaultLockManager,
        policyManager = policyManager,
        dispatcherManager = dispatcherManager,
    )
}
