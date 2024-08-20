package com.x8bit.bitwarden.data.auth.datasource.network.di

import com.x8bit.bitwarden.data.auth.datasource.network.service.AccountsService
import com.x8bit.bitwarden.data.auth.datasource.network.service.AccountsServiceImpl
import com.x8bit.bitwarden.data.auth.datasource.network.service.AuthRequestsService
import com.x8bit.bitwarden.data.auth.datasource.network.service.AuthRequestsServiceImpl
import com.x8bit.bitwarden.data.auth.datasource.network.service.DevicesService
import com.x8bit.bitwarden.data.auth.datasource.network.service.DevicesServiceImpl
import com.x8bit.bitwarden.data.auth.datasource.network.service.HaveIBeenPwnedService
import com.x8bit.bitwarden.data.auth.datasource.network.service.HaveIBeenPwnedServiceImpl
import com.x8bit.bitwarden.data.auth.datasource.network.service.IdentityService
import com.x8bit.bitwarden.data.auth.datasource.network.service.IdentityServiceImpl
import com.x8bit.bitwarden.data.auth.datasource.network.service.NewAuthRequestService
import com.x8bit.bitwarden.data.auth.datasource.network.service.NewAuthRequestServiceImpl
import com.x8bit.bitwarden.data.auth.datasource.network.service.OrganizationService
import com.x8bit.bitwarden.data.auth.datasource.network.service.OrganizationServiceImpl
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
