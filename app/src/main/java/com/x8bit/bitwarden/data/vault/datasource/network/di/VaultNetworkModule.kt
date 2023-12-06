package com.x8bit.bitwarden.data.vault.datasource.network.di

import com.x8bit.bitwarden.data.platform.datasource.network.retrofit.Retrofits
import com.x8bit.bitwarden.data.vault.datasource.network.service.CiphersService
import com.x8bit.bitwarden.data.vault.datasource.network.service.CiphersServiceImpl
import com.x8bit.bitwarden.data.vault.datasource.network.service.SyncService
import com.x8bit.bitwarden.data.vault.datasource.network.service.SyncServiceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.create
import javax.inject.Singleton

/**
 * Provides network dependencies in the vault package.
 */
@Module
@InstallIn(SingletonComponent::class)
object VaultNetworkModule {

    @Provides
    @Singleton
    fun provideCiphersService(
        retrofits: Retrofits,
    ): CiphersService = CiphersServiceImpl(
        ciphersApi = retrofits.authenticatedApiRetrofit.create(),
    )

    @Provides
    @Singleton
    fun provideSyncService(
        retrofits: Retrofits,
    ): SyncService = SyncServiceImpl(
        syncApi = retrofits.authenticatedApiRetrofit.create(),
    )
}
