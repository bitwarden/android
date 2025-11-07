package com.x8bit.bitwarden.data.vault.datasource.sdk.di

import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.sdk.Fido2CredentialStore
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.SdkClientManager
import com.x8bit.bitwarden.data.platform.manager.sdk.SdkRepositoryFactory
import com.x8bit.bitwarden.data.vault.datasource.sdk.ScopedVaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.ScopedVaultSdkSourceImpl
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSourceImpl
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.Fido2CredentialStoreImpl
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides SDK-related dependencies for the vault package.
 */
@Module
@InstallIn(SingletonComponent::class)
object VaultSdkModule {

    @Provides
    @Singleton
    fun providesVaultSdkSource(
        sdkClientManager: SdkClientManager,
        dispatcherManager: DispatcherManager,
    ): VaultSdkSource =
        VaultSdkSourceImpl(
            sdkClientManager = sdkClientManager,
            dispatcherManager = dispatcherManager,
        )

    @Provides
    fun providesScopedVaultSdkSource(
        dispatcherManager: DispatcherManager,
        featureFlagManager: FeatureFlagManager,
        sdkRepositoryFactory: SdkRepositoryFactory,
    ): ScopedVaultSdkSource =
        ScopedVaultSdkSourceImpl(
            dispatcherManager = dispatcherManager,
            featureFlagManager = featureFlagManager,
            sdkRepositoryFactory = sdkRepositoryFactory,
        )

    @Provides
    @Singleton
    fun providesFido2CredentialStore(
        authRepository: AuthRepository,
        vaultSdkSource: VaultSdkSource,
        vaultRepository: VaultRepository,
    ): Fido2CredentialStore = Fido2CredentialStoreImpl(
        authRepository = authRepository,
        vaultSdkSource = vaultSdkSource,
        vaultRepository = vaultRepository,
    )
}
