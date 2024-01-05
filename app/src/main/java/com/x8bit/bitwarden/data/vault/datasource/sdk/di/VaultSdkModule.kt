package com.x8bit.bitwarden.data.vault.datasource.sdk.di

import com.x8bit.bitwarden.data.platform.manager.SdkClientManager
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
object VaultSdkModule {

    @Provides
    @Singleton
    fun providesVaultSdkSource(
        sdkClientManager: SdkClientManager,
    ): VaultSdkSource =
        VaultSdkSourceImpl(
            sdkClientManager = sdkClientManager,
        )
}
