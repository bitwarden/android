package com.x8bit.bitwarden.data.platform.repository.di

import android.view.autofill.AutofillManager
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.data.manager.flightrecorder.FlightRecorderManager
import com.bitwarden.data.repository.ServerConfigRepository
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityEnabledManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillEnabledManager
import com.x8bit.bitwarden.data.platform.datasource.disk.EnvironmentDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.FeatureFlagOverrideDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.repository.AuthenticatorBridgeRepository
import com.x8bit.bitwarden.data.platform.repository.AuthenticatorBridgeRepositoryImpl
import com.x8bit.bitwarden.data.platform.repository.DebugMenuRepository
import com.x8bit.bitwarden.data.platform.repository.DebugMenuRepositoryImpl
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepositoryImpl
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepositoryImpl
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.ScopedVaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
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
object PlatformRepositoryModule {

    @Provides
    @Singleton
    fun providesAuthenticatorBridgeRepository(
        authDiskSource: AuthDiskSource,
        vaultDiskSource: VaultDiskSource,
        scopedVaultSdkSource: ScopedVaultSdkSource,
    ): AuthenticatorBridgeRepository = AuthenticatorBridgeRepositoryImpl(
        authDiskSource = authDiskSource,
        vaultDiskSource = vaultDiskSource,
        scopedVaultSdkSource = scopedVaultSdkSource,
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
        accessibilityEnabledManager: AccessibilityEnabledManager,
        dispatcherManager: DispatcherManager,
        policyManager: PolicyManager,
        flightRecorderManager: FlightRecorderManager,
    ): SettingsRepository =
        SettingsRepositoryImpl(
            autofillManager = autofillManager,
            autofillEnabledManager = autofillEnabledManager,
            authDiskSource = authDiskSource,
            settingsDiskSource = settingsDiskSource,
            vaultSdkSource = vaultSdkSource,
            accessibilityEnabledManager = accessibilityEnabledManager,
            dispatcherManager = dispatcherManager,
            policyManager = policyManager,
            flightRecorderManager = flightRecorderManager,
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
