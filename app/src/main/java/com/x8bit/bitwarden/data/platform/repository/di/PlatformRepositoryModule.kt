package com.x8bit.bitwarden.data.platform.repository.di

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.EnvironmentDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
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
        authDiskSource: AuthDiskSource,
        settingsDiskSource: SettingsDiskSource,
        vaultSdkSource: VaultSdkSource,
        dispatcherManager: DispatcherManager,
    ): SettingsRepository =
        SettingsRepositoryImpl(
            authDiskSource = authDiskSource,
            settingsDiskSource = settingsDiskSource,
            vaultSdkSource = vaultSdkSource,
            dispatcherManager = dispatcherManager,
        )
}
