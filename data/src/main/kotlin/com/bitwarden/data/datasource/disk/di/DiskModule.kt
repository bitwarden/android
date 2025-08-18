package com.bitwarden.data.datasource.disk.di

import android.content.SharedPreferences
import com.bitwarden.data.datasource.disk.ConfigDiskSource
import com.bitwarden.data.datasource.disk.ConfigDiskSourceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * Provides persistence-related dependencies in the data module.
 */
@Module
@InstallIn(SingletonComponent::class)
object DiskModule {
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
}
