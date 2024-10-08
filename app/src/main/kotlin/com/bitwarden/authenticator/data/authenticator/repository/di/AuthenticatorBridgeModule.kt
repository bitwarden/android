package com.bitwarden.authenticator.data.authenticator.repository.di

import android.content.Context
import com.bitwarden.authenticatorbridge.factory.AuthenticatorBridgeFactory
import com.bitwarden.authenticatorbridge.manager.AuthenticatorBridgeManager
import com.bitwarden.authenticatorbridge.manager.model.AuthenticatorBridgeConnectionType
import com.bitwarden.authenticatorbridge.model.SymmetricEncryptionKeyData
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
    ): AuthenticatorBridgeManager = factory.getAuthenticatorBridgeManager(
        connectionType = AuthenticatorBridgeConnectionType.DEV,
        symmetricKeyStorageProvider = object : SymmetricKeyStorageProvider {

            // TODO: Implement symmetric key storage: BITAU-70
            override var symmetricKey: SymmetricEncryptionKeyData? = null
        },
    )
}
