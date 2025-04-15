package com.x8bit.bitwarden.data.auth.datasource.network.di

import com.bitwarden.network.service.AccountsService
import com.bitwarden.network.service.AccountsServiceImpl
import com.bitwarden.network.service.AuthRequestsService
import com.bitwarden.network.service.AuthRequestsServiceImpl
import com.bitwarden.network.service.DevicesService
import com.bitwarden.network.service.DevicesServiceImpl
import com.bitwarden.network.service.HaveIBeenPwnedService
import com.bitwarden.network.service.HaveIBeenPwnedServiceImpl
import com.bitwarden.network.service.IdentityService
import com.bitwarden.network.service.IdentityServiceImpl
import com.bitwarden.network.service.NewAuthRequestService
import com.bitwarden.network.service.NewAuthRequestServiceImpl
import com.bitwarden.network.service.OrganizationService
import com.bitwarden.network.service.OrganizationServiceImpl
import com.x8bit.bitwarden.data.platform.datasource.network.retrofit.Retrofits
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import retrofit2.create
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
        retrofits: Retrofits,
        json: Json,
    ): AccountsService = AccountsServiceImpl(
        unauthenticatedAccountsApi = retrofits.unauthenticatedApiRetrofit.create(),
        authenticatedAccountsApi = retrofits.authenticatedApiRetrofit.create(),
        unauthenticatedKeyConnectorApi = retrofits.createStaticRetrofit().create(),
        authenticatedKeyConnectorApi = retrofits
            .createStaticRetrofit(isAuthenticated = true)
            .create(),
        json = json,
    )

    @Provides
    @Singleton
    fun providesAuthRequestsService(
        retrofits: Retrofits,
    ): AuthRequestsService = AuthRequestsServiceImpl(
        authenticatedAuthRequestsApi = retrofits.authenticatedApiRetrofit.create(),
    )

    @Provides
    @Singleton
    fun providesDevicesService(
        retrofits: Retrofits,
    ): DevicesService = DevicesServiceImpl(
        authenticatedDevicesApi = retrofits.authenticatedApiRetrofit.create(),
        unauthenticatedDevicesApi = retrofits.unauthenticatedApiRetrofit.create(),
    )

    @Provides
    @Singleton
    fun providesIdentityService(
        retrofits: Retrofits,
        json: Json,
    ): IdentityService = IdentityServiceImpl(
        unauthenticatedIdentityApi = retrofits.unauthenticatedIdentityRetrofit.create(),
        json = json,
    )

    @Provides
    @Singleton
    fun providesHaveIBeenPwnedService(
        retrofits: Retrofits,
    ): HaveIBeenPwnedService = HaveIBeenPwnedServiceImpl(
        api = retrofits
            .createStaticRetrofit(baseUrl = "https://api.pwnedpasswords.com")
            .create(),
    )

    @Provides
    @Singleton
    fun providesNewAuthRequestService(
        retrofits: Retrofits,
    ): NewAuthRequestService = NewAuthRequestServiceImpl(
        authenticatedAuthRequestsApi = retrofits.authenticatedApiRetrofit.create(),
        unauthenticatedAuthRequestsApi = retrofits.unauthenticatedApiRetrofit.create(),
    )

    @Provides
    @Singleton
    fun providesOrganizationService(
        retrofits: Retrofits,
    ): OrganizationService = OrganizationServiceImpl(
        authenticatedOrganizationApi = retrofits.authenticatedApiRetrofit.create(),
        unauthenticatedOrganizationApi = retrofits.unauthenticatedApiRetrofit.create(),
    )
}
