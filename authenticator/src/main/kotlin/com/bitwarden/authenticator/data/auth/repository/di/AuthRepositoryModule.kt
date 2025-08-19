package com.bitwarden.authenticator.data.auth.repository.di

import com.bitwarden.authenticator.data.auth.datasource.disk.AuthDiskSource
import com.bitwarden.authenticator.data.auth.repository.AuthRepository
import com.bitwarden.authenticator.data.auth.repository.AuthRepositoryImpl
import com.bitwarden.core.data.manager.realtime.RealtimeManager
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
        realtimeManager: RealtimeManager,
    ): AuthRepository = AuthRepositoryImpl(
        authDiskSource = authDiskSource,
        realtimeManager = realtimeManager,
    )
}
