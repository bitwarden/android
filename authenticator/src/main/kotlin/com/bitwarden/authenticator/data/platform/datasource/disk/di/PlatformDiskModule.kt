package com.bitwarden.authenticator.data.platform.datasource.disk.di

import android.content.SharedPreferences
import com.bitwarden.authenticator.data.platform.datasource.di.UnencryptedPreferences
import com.bitwarden.authenticator.data.platform.datasource.disk.ConfigDiskSource
import com.bitwarden.authenticator.data.platform.datasource.disk.ConfigDiskSourceImpl
import com.bitwarden.authenticator.data.platform.datasource.disk.FeatureFlagDiskSource
import com.bitwarden.authenticator.data.platform.datasource.disk.FeatureFlagDiskSourceImpl
import com.bitwarden.authenticator.data.platform.datasource.disk.FeatureFlagOverrideDiskSource
import com.bitwarden.authenticator.data.platform.datasource.disk.FeatureFlagOverrideDiskSourceImpl
import com.bitwarden.authenticator.data.platform.datasource.disk.SettingsDiskSource
import com.bitwarden.authenticator.data.platform.datasource.disk.SettingsDiskSourceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
/**
 * Provides persistence-related dependencies in the platform package.
 */
object PlatformDiskModule {

    @Provides
    @Singleton
    fun provideConfigDiskSource(
        @UnencryptedPreferences sharedPreferences: SharedPreferences,
        json: Json,
    ): ConfigDiskSource =
        ConfigDiskSourceImpl(
            sharedPreferences = sharedPreferences,
            json = json,
        )

    @Provides
    @Singleton
    fun provideSettingsDiskSource(
        @UnencryptedPreferences sharedPreferences: SharedPreferences,
    ): SettingsDiskSource =
        SettingsDiskSourceImpl(sharedPreferences = sharedPreferences)

    @Provides
    @Singleton
    fun provideFeatureFlagDiskSource(
        @UnencryptedPreferences sharedPreferences: SharedPreferences,
        json: Json,
    ): FeatureFlagDiskSource =
        FeatureFlagDiskSourceImpl(
            sharedPreferences = sharedPreferences,
            json = json,
        )

    @Provides
    @Singleton
    fun provideFeatureFlagOverrideDiskSource(
        @UnencryptedPreferences sharedPreferences: SharedPreferences,
    ): FeatureFlagOverrideDiskSource = FeatureFlagOverrideDiskSourceImpl(
        sharedPreferences = sharedPreferences,
    )
}
