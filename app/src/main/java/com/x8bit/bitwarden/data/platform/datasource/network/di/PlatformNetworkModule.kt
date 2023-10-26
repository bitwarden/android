package com.x8bit.bitwarden.data.platform.datasource.network.di

import com.x8bit.bitwarden.data.platform.datasource.network.interceptor.AuthTokenInterceptor
import com.x8bit.bitwarden.data.platform.datasource.network.interceptor.BaseUrlInterceptors
import com.x8bit.bitwarden.data.platform.datasource.network.retrofit.Retrofits
import com.x8bit.bitwarden.data.platform.datasource.network.retrofit.RetrofitsImpl
import com.x8bit.bitwarden.data.platform.datasource.network.serializer.LocalDateTimeSerializer
import com.x8bit.bitwarden.data.platform.datasource.network.service.ConfigService
import com.x8bit.bitwarden.data.platform.datasource.network.service.ConfigServiceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import retrofit2.create
import java.time.LocalDateTime
import javax.inject.Singleton

/**
 * This class provides network-related functionality for the application.
 * It initializes and configures the networking components.
 */
@Module
@InstallIn(SingletonComponent::class)
object PlatformNetworkModule {

    @Provides
    @Singleton
    fun providesConfigService(
        retrofits: Retrofits,
    ): ConfigService = ConfigServiceImpl(retrofits.unauthenticatedApiRetrofit.create())

    @Provides
    @Singleton
    fun providesAuthTokenInterceptor(): AuthTokenInterceptor = AuthTokenInterceptor()

    @Provides
    @Singleton
    fun provideRetrofits(
        authTokenInterceptor: AuthTokenInterceptor,
        baseUrlInterceptors: BaseUrlInterceptors,
        json: Json,
    ): Retrofits =
        RetrofitsImpl(
            authTokenInterceptor = authTokenInterceptor,
            baseUrlInterceptors = baseUrlInterceptors,
            json = json,
        )

    @OptIn(ExperimentalSerializationApi::class)
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
            contextual(LocalDateTime::class, LocalDateTimeSerializer())
        }
    }
}
