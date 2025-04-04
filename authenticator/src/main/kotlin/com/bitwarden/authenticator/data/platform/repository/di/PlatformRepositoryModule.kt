package com.bitwarden.authenticator.data.platform.repository.di

import com.bitwarden.authenticator.data.auth.datasource.disk.AuthDiskSource
import com.bitwarden.authenticator.data.authenticator.datasource.sdk.AuthenticatorSdkSource
import com.bitwarden.authenticator.data.platform.datasource.disk.FeatureFlagOverrideDiskSource
import com.bitwarden.authenticator.data.platform.datasource.disk.SettingsDiskSource
import com.bitwarden.authenticator.data.platform.manager.BiometricsEncryptionManager
import com.bitwarden.authenticator.data.platform.manager.DispatcherManager
import com.bitwarden.authenticator.data.platform.repository.DebugMenuRepository
import com.bitwarden.authenticator.data.platform.repository.DebugMenuRepositoryImpl
import com.bitwarden.authenticator.data.platform.repository.ServerConfigRepository
import com.bitwarden.authenticator.data.platform.repository.ServerConfigRepositoryImpl
import com.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.bitwarden.authenticator.data.platform.repository.SettingsRepositoryImpl
import com.bitwarden.data.datasource.disk.ConfigDiskSource
import com.bitwarden.network.service.ConfigService
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
    fun provideSettingsRepository(
        settingsDiskSource: SettingsDiskSource,
        authDiskSource: AuthDiskSource,
        dispatcherManager: DispatcherManager,
        biometricsEncryptionManager: BiometricsEncryptionManager,
        authenticatorSdkSource: AuthenticatorSdkSource,
    ): SettingsRepository =
        SettingsRepositoryImpl(
            settingsDiskSource = settingsDiskSource,
            authDiskSource = authDiskSource,
            dispatcherManager = dispatcherManager,
            biometricsEncryptionManager = biometricsEncryptionManager,
            authenticatorSdkSource = authenticatorSdkSource,
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
    fun provideDebugMenuRepository(
        featureFlagOverrideDiskSource: FeatureFlagOverrideDiskSource,
        serverConfigRepository: ServerConfigRepository,
    ): DebugMenuRepository = DebugMenuRepositoryImpl(
        featureFlagOverrideDiskSource = featureFlagOverrideDiskSource,
        serverConfigRepository = serverConfigRepository,
    )
}
