package com.x8bit.bitwarden.data.credentials.datasource.network.di

import com.bitwarden.network.BitwardenServiceClient
import com.bitwarden.network.service.DigitalAssetLinkService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides network dependencies in the fido2 package.
 */
@Module
@InstallIn(SingletonComponent::class)
object CredentialProviderNetworkModule {

    @Provides
    @Singleton
    fun provideDigitalAssetLinkService(
        bitwardenServiceClient: BitwardenServiceClient,
    ): DigitalAssetLinkService =
        bitwardenServiceClient.digitalAssetLinkService
}
