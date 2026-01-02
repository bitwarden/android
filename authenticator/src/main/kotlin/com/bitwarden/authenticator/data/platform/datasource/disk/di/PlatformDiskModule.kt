package com.bitwarden.authenticator.data.platform.datasource.disk.di

import android.content.SharedPreferences
import com.bitwarden.authenticator.data.platform.datasource.disk.FeatureFlagOverrideDiskSource
import com.bitwarden.authenticator.data.platform.datasource.disk.FeatureFlagOverrideDiskSourceImpl
import com.bitwarden.authenticator.data.platform.datasource.disk.SettingsDiskSource
import com.bitwarden.authenticator.data.platform.datasource.disk.SettingsDiskSourceImpl
import com.bitwarden.data.datasource.disk.FlightRecorderDiskSource
import com.bitwarden.data.datasource.disk.di.UnencryptedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
/**
 * Provides persistence-related dependencies in the platform package.
 */
object PlatformDiskModule {

    @Provides
    @Singleton
    fun provideSettingsDiskSource(
        @UnencryptedPreferences sharedPreferences: SharedPreferences,
        flightRecorderDiskSource: FlightRecorderDiskSource,
    ): SettingsDiskSource =
        SettingsDiskSourceImpl(
            sharedPreferences = sharedPreferences,
            flightRecorderDiskSource = flightRecorderDiskSource,
        )

    @Provides
    @Singleton
    fun provideFeatureFlagOverrideDiskSource(
        @UnencryptedPreferences sharedPreferences: SharedPreferences,
    ): FeatureFlagOverrideDiskSource = FeatureFlagOverrideDiskSourceImpl(
        sharedPreferences = sharedPreferences,
    )
}
