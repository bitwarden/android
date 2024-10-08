package com.bitwarden.authenticator.data.authenticator.repository.di

import android.content.Context
import com.bitwarden.authenticator.data.auth.datasource.disk.AuthDiskSource
import com.bitwarden.authenticator.data.authenticator.repository.util.SymmetricKeyStorageProviderImpl
import com.bitwarden.authenticatorbridge.factory.AuthenticatorBridgeFactory
import com.bitwarden.authenticatorbridge.manager.AuthenticatorBridgeManager
import com.bitwarden.authenticatorbridge.manager.model.AuthenticatorBridgeConnectionType
import com.bitwarden.authenticatorbridge.provider.SymmetricKeyStorageProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides repositories in the authenticator package.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthenticatorBridgeModule {

    @Provides
    @Singleton
    fun provideAuthenticatorBridgeFactory(
        @ApplicationContext
        context: Context,
    ): AuthenticatorBridgeFactory = AuthenticatorBridgeFactory(context)

    @Provides
    @Singleton
    fun provideAuthenticatorBridgeManager(
        factory: AuthenticatorBridgeFactory,
        symmetricKeyStorageProvider: SymmetricKeyStorageProvider,
    ): AuthenticatorBridgeManager = factory.getAuthenticatorBridgeManager(
        connectionType = AuthenticatorBridgeConnectionType.DEV,
        symmetricKeyStorageProvider = symmetricKeyStorageProvider,
    )

    @Provides
    fun providesSymmetricKeyStorageProvider(
        authDiskSource: AuthDiskSource,
    ): SymmetricKeyStorageProvider =
        SymmetricKeyStorageProviderImpl(
            authDiskSource = authDiskSource,
        )
}
