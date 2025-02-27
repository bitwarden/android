package com.bitwarden.authenticator.data.platform.datasource.network.di

import com.bitwarden.authenticator.data.platform.datasource.network.interceptor.BaseUrlInterceptors
import com.bitwarden.authenticator.data.platform.datasource.network.interceptor.HeadersInterceptor
import com.bitwarden.authenticator.data.platform.datasource.network.retrofit.Retrofits
import com.bitwarden.authenticator.data.platform.datasource.network.retrofit.RetrofitsImpl
import com.bitwarden.authenticator.data.platform.datasource.network.serializer.ZonedDateTimeSerializer
import com.bitwarden.authenticator.data.platform.datasource.network.service.ConfigService
import com.bitwarden.authenticator.data.platform.datasource.network.service.ConfigServiceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
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

    @Provides
    @Singleton
    fun providesJson(): Json = Json {

        // If there are keys returned by the server not modeled by a serializable class,
        // ignore them.
        // This makes additive server changes non-breaking.
        ignoreUnknownKeys = true

        // We allow for nullable values to have keys missing in the JSON response.
        explicitNulls = false
        serializersModule = SerializersModule {
            contextual(ZonedDateTimeSerializer())
        }

        // Respect model default property values.
        coerceInputValues = true
    }
}
