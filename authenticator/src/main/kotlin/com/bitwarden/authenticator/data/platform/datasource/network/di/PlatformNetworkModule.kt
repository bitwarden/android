package com.bitwarden.authenticator.data.platform.datasource.network.di

import com.bitwarden.authenticator.data.platform.datasource.network.interceptor.BaseUrlInterceptors
import com.bitwarden.authenticator.data.platform.datasource.network.interceptor.HeadersInterceptor
import com.bitwarden.authenticator.data.platform.datasource.network.retrofit.Retrofits
import com.bitwarden.authenticator.data.platform.datasource.network.retrofit.RetrofitsImpl
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
    fun providesHeadersInterceptor(): HeadersInterceptor = HeadersInterceptor()

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
