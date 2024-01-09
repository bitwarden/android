package com.x8bit.bitwarden.data.vault.manager.di

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.manager.VaultLockManager
import com.x8bit.bitwarden.data.vault.manager.VaultLockManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides managers in the vault package.
 */
@Module
@InstallIn(SingletonComponent::class)
object VaultManagerModule {

    @Provides
    @Singleton
    fun provideVaultLockManager(
        authDiskSource: AuthDiskSource,
        vaultSdkSource: VaultSdkSource,
    ): VaultLockManager =
        VaultLockManagerImpl(
            authDiskSource = authDiskSource,
            vaultSdkSource = vaultSdkSource,
        )
}
