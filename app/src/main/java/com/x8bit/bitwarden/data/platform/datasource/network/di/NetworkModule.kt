package com.x8bit.bitwarden.data.platform.datasource.network.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
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
    fun provideConfigApiService(retrofit: Retrofit): ConfigApi {
        return retrofit.create(ConfigApi::class.java)
    }

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
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl("https://api.bitwarden.com")
            .client(okHttpClient)
            .addConverterFactory(Json.asConverterFactory(contentType))
            .addCallAdapterFactory(ResultCallAdapterFactory())
            .build()
    }
}
