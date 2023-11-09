package com.x8bit.bitwarden.data.generator.datasource.sdk.di

import com.bitwarden.sdk.Client
import com.x8bit.bitwarden.data.generator.datasource.sdk.GeneratorSdkSource
import com.x8bit.bitwarden.data.generator.datasource.sdk.GeneratorSdkSourceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides SDK-related dependencies for the password generation package.
 */
@Module
@InstallIn(SingletonComponent::class)
object GeneratorSdkModule {

    @Provides
    @Singleton
    fun provideGeneratorSdkSource(
        client: Client,
    ): GeneratorSdkSource = GeneratorSdkSourceImpl(clientGenerator = client.generators())
}
