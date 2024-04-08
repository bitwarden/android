package com.x8bit.bitwarden.data.tools.generator.datasource.sdk.di

import com.x8bit.bitwarden.data.platform.manager.SdkClientManager
import com.x8bit.bitwarden.data.tools.generator.datasource.sdk.GeneratorSdkSource
import com.x8bit.bitwarden.data.tools.generator.datasource.sdk.GeneratorSdkSourceImpl
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
        sdkClientManager: SdkClientManager,
    ): GeneratorSdkSource = GeneratorSdkSourceImpl(sdkClientManager = sdkClientManager)
}
