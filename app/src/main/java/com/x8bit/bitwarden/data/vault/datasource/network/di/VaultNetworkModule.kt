package com.x8bit.bitwarden.data.vault.datasource.network.di

import com.bitwarden.network.BitwardenServiceClient
import com.bitwarden.network.service.CiphersService
import com.bitwarden.network.service.DownloadService
import com.bitwarden.network.service.FolderService
import com.bitwarden.network.service.SendsService
import com.bitwarden.network.service.SyncService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
        bitwardenServiceClient: BitwardenServiceClient,
    ): CiphersService = bitwardenServiceClient.ciphersService

    @Provides
    @Singleton
    fun providesFolderService(
        bitwardenServiceClient: BitwardenServiceClient,
    ): FolderService = bitwardenServiceClient.folderService

    @Provides
    @Singleton
    fun provideSendsService(
        bitwardenServiceClient: BitwardenServiceClient,
    ): SendsService = bitwardenServiceClient.sendsService

    @Provides
    @Singleton
    fun provideSyncService(
        bitwardenServiceClient: BitwardenServiceClient,
    ): SyncService = bitwardenServiceClient.syncService

    @Provides
    @Singleton
    fun provideDownloadService(
        bitwardenServiceClient: BitwardenServiceClient,
    ): DownloadService = bitwardenServiceClient.downloadService
}
