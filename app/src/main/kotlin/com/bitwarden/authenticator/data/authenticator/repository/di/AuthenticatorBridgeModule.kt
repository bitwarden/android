package com.bitwarden.authenticator.data.authenticator.repository.di

import android.content.Context
import com.bitwarden.authenticator.BuildConfig
import com.bitwarden.authenticator.data.auth.datasource.disk.AuthDiskSource
import com.bitwarden.authenticator.data.authenticator.repository.util.SymmetricKeyStorageProviderImpl
import com.bitwarden.authenticator.data.platform.manager.FeatureFlagManager
import com.bitwarden.authenticator.data.platform.manager.model.LocalFeatureFlag
import com.bitwarden.authenticatorbridge.factory.AuthenticatorBridgeFactory
import com.bitwarden.authenticatorbridge.manager.AuthenticatorBridgeManager
import com.bitwarden.authenticatorbridge.manager.model.AccountSyncState
import com.bitwarden.authenticatorbridge.provider.SymmetricKeyStorageProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
        featureFlagManager: FeatureFlagManager,
    ): AuthenticatorBridgeManager =
        if (featureFlagManager.getFeatureFlag(LocalFeatureFlag.PasswordManagerSync)) {
            factory.getAuthenticatorBridgeManager(
                connectionType = BuildConfig.AUTHENTICATOR_BRIDGE_CONNECTION_TYPE,
                symmetricKeyStorageProvider = symmetricKeyStorageProvider,
            )
        } else {
            // If feature flag is not enabled, return no-op bridge manager so we never
            // connect to bridge service:
            object : AuthenticatorBridgeManager {
                override val accountSyncStateFlow: StateFlow<AccountSyncState>
                    get() = MutableStateFlow(AccountSyncState.Loading)
            }
        }

    @Provides
    fun providesSymmetricKeyStorageProvider(
        authDiskSource: AuthDiskSource,
    ): SymmetricKeyStorageProvider =
        SymmetricKeyStorageProviderImpl(
            authDiskSource = authDiskSource,
        )
}
