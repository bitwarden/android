package com.x8bit.bitwarden.data.generator.datasource.disk.di

import android.content.SharedPreferences
import com.x8bit.bitwarden.data.generator.datasource.disk.GeneratorDiskSource
import com.x8bit.bitwarden.data.generator.datasource.disk.GeneratorDiskSourceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * Provides persistence-related dependencies for the generator package.
 */
@Module
@InstallIn(SingletonComponent::class)
object GeneratorDiskModule {

    @Provides
    @Singleton
    fun provideGeneratorDiskSource(
        sharedPreferences: SharedPreferences,
        json: Json,
    ): GeneratorDiskSource =
        GeneratorDiskSourceImpl(
            sharedPreferences = sharedPreferences,
            json = json,
        )
}
