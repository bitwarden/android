package com.bitwarden.authenticator.data.platform.datasource.network.retrofit

import com.bitwarden.authenticator.data.platform.datasource.network.interceptor.BaseUrlInterceptors
import retrofit2.Retrofit
import retrofit2.http.Url

/**
 * A collection of various [Retrofit] instances that serve different purposes.
 */
interface Retrofits {

    /**
     * Allows access to "/api" calls that do not require authentication.
     *
     * The base URL can be dynamically determined via the [BaseUrlInterceptors].
     */
    val unauthenticatedApiRetrofit: Retrofit

    /**
     * Allows access to static API calls (ex: external APIs).
     *
     * @param isAuthenticated Indicates if the [Retrofit] instance should use authentication.
     * @param baseUrl The static base url associated with this retrofit instance. This can be
     * overridden with the [Url] annotation.
     */
    fun createStaticRetrofit(
        isAuthenticated: Boolean = false,
        baseUrl: String = "https://api.bitwarden.com",
    ): Retrofit
}
