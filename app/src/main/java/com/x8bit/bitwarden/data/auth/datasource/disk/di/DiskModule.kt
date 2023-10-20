package com.x8bit.bitwarden.data.auth.datasource.disk.di

import android.content.SharedPreferences
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSourceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * Provides persistence-related dependencies in the auth package.
 */
@Module
@InstallIn(SingletonComponent::class)
object DiskModule {

    @Provides
    @Singleton
    fun provideAuthDiskSource(
        sharedPreferences: SharedPreferences,
        json: Json,
    ): AuthDiskSource =
        AuthDiskSourceImpl(
            sharedPreferences = sharedPreferences,
            json = json,
        )
}
