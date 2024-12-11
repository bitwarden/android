package com.x8bit.bitwarden.data.tools.generator.repository.di

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.platform.manager.ReviewPromptManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.tools.generator.datasource.disk.GeneratorDiskSource
import com.x8bit.bitwarden.data.tools.generator.datasource.disk.PasswordHistoryDiskSource
import com.x8bit.bitwarden.data.tools.generator.datasource.sdk.GeneratorSdkSource
import com.x8bit.bitwarden.data.tools.generator.repository.GeneratorRepository
import com.x8bit.bitwarden.data.tools.generator.repository.GeneratorRepositoryImpl
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
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
        clock: Clock,
        generatorSdkSource: GeneratorSdkSource,
        generatorDiskSource: GeneratorDiskSource,
        authDiskSource: AuthDiskSource,
        vaultSdkSource: VaultSdkSource,
        passwordHistoryDiskSource: PasswordHistoryDiskSource,
        dispatcherManager: DispatcherManager,
        reviewPromptManager: ReviewPromptManager,
    ): GeneratorRepository = GeneratorRepositoryImpl(
        clock = clock,
        generatorSdkSource = generatorSdkSource,
        generatorDiskSource = generatorDiskSource,
        authDiskSource = authDiskSource,
        vaultSdkSource = vaultSdkSource,
        passwordHistoryDiskSource = passwordHistoryDiskSource,
        dispatcherManager = dispatcherManager,
        reviewPromptManager = reviewPromptManager,
    )
}
