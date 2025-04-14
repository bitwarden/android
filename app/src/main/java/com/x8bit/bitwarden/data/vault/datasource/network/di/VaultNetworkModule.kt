package com.x8bit.bitwarden.data.vault.datasource.network.di

import com.bitwarden.network.service.CiphersService
import com.bitwarden.network.service.CiphersServiceImpl
import com.bitwarden.network.service.DownloadService
import com.bitwarden.network.service.DownloadServiceImpl
import com.bitwarden.network.service.FolderService
import com.bitwarden.network.service.FolderServiceImpl
import com.bitwarden.network.service.SendsService
import com.bitwarden.network.service.SendsServiceImpl
import com.bitwarden.network.service.SyncService
import com.bitwarden.network.service.SyncServiceImpl
import com.x8bit.bitwarden.data.platform.datasource.network.retrofit.Retrofits
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import retrofit2.create
import java.time.Clock
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
        json: Json,
        clock: Clock,
    ): CiphersService = CiphersServiceImpl(
        azureApi = retrofits
            .createStaticRetrofit()
            .create(),
        ciphersApi = retrofits.authenticatedApiRetrofit.create(),
        json = json,
        clock = clock,
    )

    @Provides
    @Singleton
    fun providesFolderService(
        retrofits: Retrofits,
        json: Json,
    ): FolderService = FolderServiceImpl(
        foldersApi = retrofits.authenticatedApiRetrofit.create(),
        json = json,
    )

    @Provides
    @Singleton
    fun provideSendsService(
        retrofits: Retrofits,
        json: Json,
        clock: Clock,
    ): SendsService = SendsServiceImpl(
        azureApi = retrofits
            .createStaticRetrofit()
            .create(),
        sendsApi = retrofits.authenticatedApiRetrofit.create(),
        json = json,
        clock = clock,
    )

    @Provides
    @Singleton
    fun provideSyncService(
        retrofits: Retrofits,
    ): SyncService = SyncServiceImpl(
        syncApi = retrofits.authenticatedApiRetrofit.create(),
    )

    @Provides
    @Singleton
    fun provideDownloadService(
        retrofits: Retrofits,
    ): DownloadService = DownloadServiceImpl(
        downloadApi = retrofits
            .createStaticRetrofit()
            .create(),
    )
}
