package com.bitwarden.authenticator.data.auth.repository.di

import com.bitwarden.authenticator.data.auth.datasource.disk.AuthDiskSource
import com.bitwarden.authenticator.data.auth.repository.AuthRepository
import com.bitwarden.authenticator.data.auth.repository.AuthRepositoryImpl
import com.bitwarden.authenticator.data.authenticator.datasource.sdk.AuthenticatorSdkSource
import com.bitwarden.authenticator.data.platform.manager.BiometricsEncryptionManager
import com.bitwarden.authenticator.data.platform.manager.lock.AppLockManager
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
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
        authenticatorSdkSource: AuthenticatorSdkSource,
        biometricsEncryptionManager: BiometricsEncryptionManager,
        realtimeManager: RealtimeManager,
        dispatcherManager: DispatcherManager,
        appLockManager: AppLockManager,
    ): AuthRepository = AuthRepositoryImpl(
        authDiskSource = authDiskSource,
        authenticatorSdkSource = authenticatorSdkSource,
        biometricsEncryptionManager = biometricsEncryptionManager,
        realtimeManager = realtimeManager,
        dispatcherManager = dispatcherManager,
        appLockManager = appLockManager,
    )
}
