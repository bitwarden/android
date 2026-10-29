package com.x8bit.bitwarden.data.pam.repository.di

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.pam.datasource.sdk.PamSdkSource
import com.x8bit.bitwarden.data.pam.repository.PamRepository
import com.x8bit.bitwarden.data.pam.repository.PamRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides repositories in the PAM package.
 */
@Module
@InstallIn(SingletonComponent::class)
object PamRepositoryModule {

    @Provides
    @Singleton
    fun providesPamRepository(
        authDiskSource: AuthDiskSource,
        pamSdkSource: PamSdkSource,
    ): PamRepository = PamRepositoryImpl(
        authDiskSource = authDiskSource,
        pamSdkSource = pamSdkSource,
    )
}
