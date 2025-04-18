package com.bitwarden.authenticator.data.platform.datasource.network.di

import com.bitwarden.authenticator.data.platform.datasource.network.retrofit.Retrofits
import com.bitwarden.authenticator.data.platform.datasource.network.retrofit.RetrofitsImpl
import com.bitwarden.authenticator.data.platform.datasource.network.util.HEADER_VALUE_CLIENT_NAME
import com.bitwarden.authenticator.data.platform.datasource.network.util.HEADER_VALUE_CLIENT_VERSION
import com.bitwarden.authenticator.data.platform.datasource.network.util.HEADER_VALUE_USER_AGENT
import com.bitwarden.network.interceptor.BaseUrlInterceptors
import com.bitwarden.network.interceptor.BaseUrlsProvider
import com.bitwarden.network.interceptor.HeadersInterceptor
import com.bitwarden.network.service.ConfigService
import com.bitwarden.network.service.ConfigServiceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import retrofit2.create
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
/**
 * This class provides network-related functionality for the application.
 * It initializes and configures the networking components.
 */
object PlatformNetworkModule {
    @Provides
    @Singleton
    fun providesConfigService(
        retrofits: Retrofits,
    ): ConfigService = ConfigServiceImpl(retrofits.unauthenticatedApiRetrofit.create())

    @Provides
    @Singleton
    fun providesHeadersInterceptor(): HeadersInterceptor = HeadersInterceptor(
        userAgent = HEADER_VALUE_USER_AGENT,
        clientName = HEADER_VALUE_CLIENT_NAME,
        clientVersion = HEADER_VALUE_CLIENT_VERSION,
    )

    @Provides
    @Singleton
    fun providesBaseUrlInterceptors(
        baseUrlsProvider: BaseUrlsProvider,
    ): BaseUrlInterceptors =
        BaseUrlInterceptors(baseUrlsProvider = baseUrlsProvider)

    @Provides
    @Singleton
    fun provideRetrofits(
        baseUrlInterceptors: BaseUrlInterceptors,
        headersInterceptor: HeadersInterceptor,
        json: Json,
    ): Retrofits =
        RetrofitsImpl(
            baseUrlInterceptors = baseUrlInterceptors,
            headersInterceptor = headersInterceptor,
            json = json,
        )
}
