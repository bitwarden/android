package com.x8bit.bitwarden.data.platform.repository.di

import android.view.autofill.AutofillManager
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityEnabledManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillEnabledManager
import com.x8bit.bitwarden.data.platform.datasource.disk.ConfigDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.EnvironmentDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.FeatureFlagOverrideDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.datasource.network.service.ConfigService
import com.x8bit.bitwarden.data.platform.manager.BiometricsEncryptionManager
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.AuthenticatorBridgeRepository
import com.x8bit.bitwarden.data.platform.repository.AuthenticatorBridgeRepositoryImpl
import com.x8bit.bitwarden.data.platform.repository.DebugMenuRepository
import com.x8bit.bitwarden.data.platform.repository.DebugMenuRepositoryImpl
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepositoryImpl
import com.x8bit.bitwarden.data.platform.repository.ServerConfigRepository
import com.x8bit.bitwarden.data.platform.repository.ServerConfigRepositoryImpl
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepositoryImpl
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
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
object PlatformRepositoryModule {

    @Provides
    @Singleton
    fun providesAuthenticatorBridgeRepository(
        authRepository: AuthRepository,
        authDiskSource: AuthDiskSource,
        vaultRepository: VaultRepository,
        vaultDiskSource: VaultDiskSource,
        vaultSdkSource: VaultSdkSource,
    ): AuthenticatorBridgeRepository = AuthenticatorBridgeRepositoryImpl(
        authRepository = authRepository,
        authDiskSource = authDiskSource,
        vaultRepository = vaultRepository,
        vaultDiskSource = vaultDiskSource,
        vaultSdkSource = vaultSdkSource,
    )

    @Provides
    @Singleton
    fun provideServerConfigRepository(
        configDiskSource: ConfigDiskSource,
        configService: ConfigService,
        clock: Clock,
        dispatcherManager: DispatcherManager,
    ): ServerConfigRepository =
        ServerConfigRepositoryImpl(
            configDiskSource = configDiskSource,
            configService = configService,
            clock = clock,
            dispatcherManager = dispatcherManager,
        )

    @Provides
    @Singleton
    fun provideEnvironmentRepository(
        environmentDiskSource: EnvironmentDiskSource,
        authDiskSource: AuthDiskSource,
        dispatcherManager: DispatcherManager,
    ): EnvironmentRepository =
        EnvironmentRepositoryImpl(
            environmentDiskSource = environmentDiskSource,
            authDiskSource = authDiskSource,
            dispatcherManager = dispatcherManager,
        )

    @Provides
    @Singleton
    fun provideSettingsRepository(
        autofillManager: AutofillManager,
        autofillEnabledManager: AutofillEnabledManager,
        authDiskSource: AuthDiskSource,
        settingsDiskSource: SettingsDiskSource,
        vaultSdkSource: VaultSdkSource,
        encryptionManager: BiometricsEncryptionManager,
        accessibilityEnabledManager: AccessibilityEnabledManager,
        dispatcherManager: DispatcherManager,
        policyManager: PolicyManager,
    ): SettingsRepository =
        SettingsRepositoryImpl(
            autofillManager = autofillManager,
            autofillEnabledManager = autofillEnabledManager,
            authDiskSource = authDiskSource,
            settingsDiskSource = settingsDiskSource,
            vaultSdkSource = vaultSdkSource,
            biometricsEncryptionManager = encryptionManager,
            accessibilityEnabledManager = accessibilityEnabledManager,
            dispatcherManager = dispatcherManager,
            policyManager = policyManager,
        )

    @Provides
    @Singleton
    fun provideDebugMenuRepository(
        featureFlagOverrideDiskSource: FeatureFlagOverrideDiskSource,
        serverConfigRepository: ServerConfigRepository,
        authDiskSource: AuthDiskSource,
        settingsDiskSource: SettingsDiskSource,
    ): DebugMenuRepository = DebugMenuRepositoryImpl(
        featureFlagOverrideDiskSource = featureFlagOverrideDiskSource,
        serverConfigRepository = serverConfigRepository,
        authDiskSource = authDiskSource,
        settingsDiskSource = settingsDiskSource,
    )
}
