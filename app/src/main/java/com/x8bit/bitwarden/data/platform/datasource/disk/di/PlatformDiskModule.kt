package com.x8bit.bitwarden.data.platform.datasource.disk.di

import android.content.SharedPreferences
import com.x8bit.bitwarden.data.platform.datasource.disk.EnvironmentDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.EnvironmentDiskSourceImpl
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSourceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * Provides persistence-related dependencies in the platform package.
 */
@Module
@InstallIn(SingletonComponent::class)
object PlatformDiskModule {

    @Provides
    @Singleton
    fun provideEnvironmentDiskSource(
        sharedPreferences: SharedPreferences,
        json: Json,
    ): EnvironmentDiskSource =
        EnvironmentDiskSourceImpl(
            sharedPreferences = sharedPreferences,
            json = json,
        )

    @Provides
    @Singleton
    fun provideSettingsDiskSource(
        sharedPreferences: SharedPreferences,
    ): SettingsDiskSource =
        SettingsDiskSourceImpl(
            sharedPreferences = sharedPreferences,
        )
}
