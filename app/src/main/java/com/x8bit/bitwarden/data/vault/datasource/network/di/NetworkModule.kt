package com.x8bit.bitwarden.data.vault.datasource.network.di

import com.x8bit.bitwarden.data.platform.datasource.network.di.NetworkModule
import com.x8bit.bitwarden.data.vault.datasource.network.api.SyncApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Named
import javax.inject.Singleton

/**
 * Provides network dependencies in the vault package.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideSyncApiService(@Named(NetworkModule.AUTHORIZED) retrofit: Retrofit): SyncApi =
        retrofit.create()
}
