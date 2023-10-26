package com.x8bit.bitwarden.data.vault.datasource.network.di

import com.x8bit.bitwarden.data.platform.datasource.network.retrofit.Retrofits
import com.x8bit.bitwarden.data.vault.datasource.network.api.SyncApi
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
    fun provideSyncApiService(
        retrofits: Retrofits,
    ): SyncApi = retrofits.authenticatedApiRetrofit.create()
}
