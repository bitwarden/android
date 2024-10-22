package com.x8bit.bitwarden.data.auth.repository.di

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.network.service.AccountsService
import com.x8bit.bitwarden.data.auth.datasource.network.service.DevicesService
import com.x8bit.bitwarden.data.auth.datasource.network.service.HaveIBeenPwnedService
import com.x8bit.bitwarden.data.auth.datasource.network.service.IdentityService
import com.x8bit.bitwarden.data.auth.datasource.network.service.OrganizationService
import com.x8bit.bitwarden.data.auth.datasource.sdk.AuthSdkSource
import com.x8bit.bitwarden.data.auth.manager.AuthRequestManager
import com.x8bit.bitwarden.data.auth.manager.KeyConnectorManager
import com.x8bit.bitwarden.data.auth.manager.TrustedDeviceManager
import com.x8bit.bitwarden.data.auth.manager.UserLogoutManager
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.AuthRepositoryImpl
import com.x8bit.bitwarden.data.platform.datasource.disk.ConfigDiskSource
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManager
import com.x8bit.bitwarden.data.platform.manager.LogsManager
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
        accountsService: AccountsService,
        devicesService: DevicesService,
        identityService: IdentityService,
        haveIBeenPwnedService: HaveIBeenPwnedService,
        organizationService: OrganizationService,
        authSdkSource: AuthSdkSource,
        vaultSdkSource: VaultSdkSource,
        authDiskSource: AuthDiskSource,
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
        featureFlagManager: FeatureFlagManager,
        firstTimeActionManager: FirstTimeActionManager,
        logsManager: LogsManager,
    ): AuthRepository = AuthRepositoryImpl(
        accountsService = accountsService,
        devicesService = devicesService,
        identityService = identityService,
        organizationService = organizationService,
        authSdkSource = authSdkSource,
        vaultSdkSource = vaultSdkSource,
        authDiskSource = authDiskSource,
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
        featureFlagManager = featureFlagManager,
        firstTimeActionManager = firstTimeActionManager,
        logsManager = logsManager,
    )
}
