package com.x8bit.bitwarden.data.platform.repository.di

import android.view.autofill.AutofillManager
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.EnvironmentDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.AppForegroundManager
import com.x8bit.bitwarden.data.platform.manager.BiometricsEncryptionManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepositoryImpl
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepositoryImpl
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
        appForegroundManager: AppForegroundManager,
        authDiskSource: AuthDiskSource,
        settingsDiskSource: SettingsDiskSource,
        vaultSdkSource: VaultSdkSource,
        encryptionManager: BiometricsEncryptionManager,
        dispatcherManager: DispatcherManager,
    ): SettingsRepository =
        SettingsRepositoryImpl(
            autofillManager = autofillManager,
            appForegroundManager = appForegroundManager,
            authDiskSource = authDiskSource,
            settingsDiskSource = settingsDiskSource,
            vaultSdkSource = vaultSdkSource,
            biometricsEncryptionManager = encryptionManager,
            dispatcherManager = dispatcherManager,
        )
}
