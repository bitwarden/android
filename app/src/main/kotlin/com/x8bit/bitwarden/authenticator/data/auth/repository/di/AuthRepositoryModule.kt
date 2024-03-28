package com.x8bit.bitwarden.authenticator.data.auth.repository.di

import com.x8bit.bitwarden.authenticator.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.authenticator.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.authenticator.data.auth.repository.AuthRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Provides repositories in the auth package.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthRepositoryModule {

    @Provides
    fun provideAuthRepository(
        authDiskSource: AuthDiskSource,
    ): AuthRepository = AuthRepositoryImpl(
        authDiskSource = authDiskSource
    )
}
