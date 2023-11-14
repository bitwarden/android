package com.x8bit.bitwarden.data.tools.generator.repository.di

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.tools.generator.datasource.disk.GeneratorDiskSource
import com.x8bit.bitwarden.data.tools.generator.datasource.sdk.GeneratorSdkSource
import com.x8bit.bitwarden.data.tools.generator.repository.GeneratorRepository
import com.x8bit.bitwarden.data.tools.generator.repository.GeneratorRepositoryImpl
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
        generatorDiskSource: GeneratorDiskSource,
        authDiskSource: AuthDiskSource,
    ): GeneratorRepository = GeneratorRepositoryImpl(
        generatorSdkSource = generatorSdkSource,
        generatorDiskSource = generatorDiskSource,
        authDiskSource = authDiskSource,
    )
}
