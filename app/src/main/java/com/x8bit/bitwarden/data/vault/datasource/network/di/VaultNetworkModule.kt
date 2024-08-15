package com.x8bit.bitwarden.data.vault.datasource.network.di

import com.x8bit.bitwarden.data.platform.datasource.network.retrofit.Retrofits
import com.x8bit.bitwarden.data.vault.datasource.network.service.CiphersService
import com.x8bit.bitwarden.data.vault.datasource.network.service.CiphersServiceImpl
import com.x8bit.bitwarden.data.vault.datasource.network.service.DownloadService
import com.x8bit.bitwarden.data.vault.datasource.network.service.DownloadServiceImpl
import com.x8bit.bitwarden.data.vault.datasource.network.service.FolderService
import com.x8bit.bitwarden.data.vault.datasource.network.service.FolderServiceImpl
import com.x8bit.bitwarden.data.vault.datasource.network.service.SendsService
import com.x8bit.bitwarden.data.vault.datasource.network.service.SendsServiceImpl
import com.x8bit.bitwarden.data.vault.datasource.network.service.SyncService
import com.x8bit.bitwarden.data.vault.datasource.network.service.SyncServiceImpl
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
