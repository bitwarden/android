package com.x8bit.bitwarden.data.vault.datasource.sdk.di

import com.bitwarden.sdk.Client
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSourceImpl
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
class VaultSdkModule {

    @Provides
    @Singleton
    fun providesVaultSdkSource(
        client: Client,
    ): VaultSdkSource = VaultSdkSourceImpl(clientVault = client.vault())
}
