package com.x8bit.bitwarden.data.platform.datasource.network.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.x8bit.bitwarden.data.auth.datasource.network.api.AccountsApi
import com.x8bit.bitwarden.data.auth.datasource.network.api.IdentityApi
import com.x8bit.bitwarden.data.platform.datasource.network.api.ConfigApi
import com.x8bit.bitwarden.data.platform.datasource.network.core.ResultCallAdapterFactory
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
import javax.inject.Singleton

/**
 * This class provides network-related functionality for the application.
 * It initializes and configures the networking components.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun providesAccountsApiService(retrofit: Retrofit): AccountsApi = retrofit.create()

    @Provides
    @Singleton
    fun providesConfigApiService(retrofit: Retrofit): ConfigApi = retrofit.create()

    @Provides
    @Singleton
    fun providesIdentityApiService(retrofit: Retrofit): IdentityApi = retrofit.create()

    @Provides
    @Singleton
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
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit {
        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl("https://api.bitwarden.com")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .addCallAdapterFactory(ResultCallAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun providesJson(): Json = Json {

        // If there are keys returned by the server not modeled by a serializable class,
        // ignore them.
        // This makes additive server changes non-breaking.
        ignoreUnknownKeys = true
    }
}
