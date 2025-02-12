package com.bitwarden.authenticator.data.platform.datasource.network.retrofit

import com.bitwarden.authenticator.data.platform.datasource.network.core.ResultCallAdapterFactory
import com.bitwarden.authenticator.data.platform.datasource.network.interceptor.BaseUrlInterceptor
import com.bitwarden.authenticator.data.platform.datasource.network.interceptor.BaseUrlInterceptors
import com.bitwarden.authenticator.data.platform.datasource.network.interceptor.HeadersInterceptor
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Primary implementation of [Retrofits].
 */
class RetrofitsImpl(
    baseUrlInterceptors: BaseUrlInterceptors,
    headersInterceptor: HeadersInterceptor,
    json: Json,
) : Retrofits {

    //region Unauthenticated Retrofits

    override val unauthenticatedApiRetrofit: Retrofit by lazy {
        createUnauthenticatedRetrofit(
            baseUrlInterceptor = baseUrlInterceptors.apiInterceptor,
        )
    }

    //endregion Unauthenticated Retrofits

    //region Static Retrofit

    override fun createStaticRetrofit(isAuthenticated: Boolean, baseUrl: String): Retrofit {
        return baseRetrofitBuilder
            .baseUrl(baseUrl)
            .client(
                baseOkHttpClient
                    .newBuilder()
                    .build(),
            )
            .build()
    }

    //endregion Static Retrofit

    //region Helper properties and functions

    private val baseOkHttpClient: OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(headersInterceptor)
            .build()

    private val baseRetrofit: Retrofit by lazy {
        baseRetrofitBuilder
            .baseUrl("https://api.bitwarden.com")
            .build()
    }

    private val baseRetrofitBuilder: Retrofit.Builder by lazy {
        Retrofit.Builder()
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .addCallAdapterFactory(ResultCallAdapterFactory())
            .client(baseOkHttpClient)
    }

    private fun createUnauthenticatedRetrofit(
        baseUrlInterceptor: BaseUrlInterceptor,
    ): Retrofit =
        baseRetrofit
            .newBuilder()
            .client(
                baseOkHttpClient
                    .newBuilder()
                    .addInterceptor(baseUrlInterceptor)
                    .build(),
            )
            .build()

    //endregion Helper properties and functions
}
