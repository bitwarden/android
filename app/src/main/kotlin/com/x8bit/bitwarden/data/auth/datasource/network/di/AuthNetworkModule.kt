package com.x8bit.bitwarden.data.auth.datasource.network.di

import com.bitwarden.network.BitwardenServiceClient
import com.bitwarden.network.service.AccountsService
import com.bitwarden.network.service.AuthRequestsService
import com.bitwarden.network.service.DevicesService
import com.bitwarden.network.service.HaveIBeenPwnedService
import com.bitwarden.network.service.IdentityService
import com.bitwarden.network.service.NewAuthRequestService
import com.bitwarden.network.service.OrganizationService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides network dependencies in the auth package.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthNetworkModule {

    @Provides
    @Singleton
    fun providesAccountService(
        bitwardenServiceClient: BitwardenServiceClient,
    ): AccountsService = bitwardenServiceClient.accountsService

    @Provides
    @Singleton
    fun providesAuthRequestsService(
        bitwardenServiceClient: BitwardenServiceClient,
    ): AuthRequestsService = bitwardenServiceClient.authRequestsService

    @Provides
    @Singleton
    fun providesDevicesService(
        bitwardenServiceClient: BitwardenServiceClient,
    ): DevicesService = bitwardenServiceClient.devicesService

    @Provides
    @Singleton
    fun providesIdentityService(
        bitwardenServiceClient: BitwardenServiceClient,
    ): IdentityService = bitwardenServiceClient.identityService

    @Provides
    @Singleton
    fun providesHaveIBeenPwnedService(
        bitwardenServiceClient: BitwardenServiceClient,
    ): HaveIBeenPwnedService = bitwardenServiceClient.haveIBeenPwnedService

    @Provides
    @Singleton
    fun providesNewAuthRequestService(
        bitwardenServiceClient: BitwardenServiceClient,
    ): NewAuthRequestService = bitwardenServiceClient.newAuthRequestService

    @Provides
    @Singleton
    fun providesOrganizationService(
        bitwardenServiceClient: BitwardenServiceClient,
    ): OrganizationService = bitwardenServiceClient.organizationService
}
