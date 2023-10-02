package com.x8bit.bitwarden.data.platform.datasource.network.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.x8bit.bitwarden.data.platform.datasource.network.core.ResultCallAdapterFactory
import com.x8bit.bitwarden.data.platform.datasource.network.interceptor.AuthTokenInterceptor
import com.x8bit.bitwarden.data.platform.datasource.network.service.ConfigService
import com.x8bit.bitwarden.data.platform.datasource.network.service.ConfigServiceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Named
import javax.inject.Singleton

/**
 * This class provides network-related functionality for the application.
 * It initializes and configures the networking components.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    const val AUTHORIZED: String = "authorized"
    const val UNAUTHORIZED: String = "unauthorized"

    @Provides
    @Singleton
    fun providesConfigService(@Named(UNAUTHORIZED) retrofit: Retrofit): ConfigService =
        ConfigServiceImpl(retrofit.create())

    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    setLevel(HttpLoggingInterceptor.Level.BODY)
                },
            )
            .build()
    }

    @Provides
    @Singleton
    fun providesAuthTokenInterceptor(): AuthTokenInterceptor = AuthTokenInterceptor()

    @Provides
    @Singleton
    fun providesOkHttpClientBuilder(): OkHttpClient.Builder =
        OkHttpClient.Builder().addInterceptor(
            HttpLoggingInterceptor().apply {
                setLevel(HttpLoggingInterceptor.Level.BODY)
            },
        )

    @Provides
    @Singleton
    fun providesRetrofitBuilder(
        json: Json,
    ): Retrofit.Builder {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder().baseUrl("https://api.bitwarden.com")
            .addConverterFactory(json.asConverterFactory(contentType))
            .addCallAdapterFactory(ResultCallAdapterFactory())
    }

    @Provides
    @Singleton
    @Named(UNAUTHORIZED)
    fun providesUnauthorizedRetrofit(
        okHttpClientBuilder: OkHttpClient.Builder,
        retrofitBuilder: Retrofit.Builder,
    ): Retrofit =
        retrofitBuilder
            .client(
                okHttpClientBuilder.build(),
            )
            .build()

    @Provides
    @Singleton
    @Named(AUTHORIZED)
    fun providesAuthorizedRetrofit(
        okHttpClientBuilder: OkHttpClient.Builder,
        retrofitBuilder: Retrofit.Builder,
        authTokenInterceptor: AuthTokenInterceptor,
    ): Retrofit =
        retrofitBuilder
            .client(
                okHttpClientBuilder.addInterceptor(authTokenInterceptor).build(),
            )
            .build()

    @Provides
    @Singleton
    fun providesJson(): Json = Json {

        // If there are keys returned by the server not modeled by a serializable class,
        // ignore them.
        // This makes additive server changes non-breaking.
        ignoreUnknownKeys = true
    }
}
