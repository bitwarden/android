package com.x8bit.bitwarden.data.generator.repository.di

import com.x8bit.bitwarden.data.generator.datasource.sdk.GeneratorSdkSource
import com.x8bit.bitwarden.data.generator.repository.GeneratorRepository
import com.x8bit.bitwarden.data.generator.repository.GeneratorRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides repositories in the generator package.
 */
@Module
@InstallIn(SingletonComponent::class)
object GeneratorRepositoryModule {

    @Provides
    @Singleton
    fun provideGeneratorRepository(
        generatorSdkSource: GeneratorSdkSource,
    ): GeneratorRepository = GeneratorRepositoryImpl(generatorSdkSource)
}
