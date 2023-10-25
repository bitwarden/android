package com.x8bit.bitwarden.data.platform.datasource.network.retrofit

import com.x8bit.bitwarden.data.platform.datasource.network.interceptor.BaseUrlInterceptors
import retrofit2.Retrofit

/**
 * A collection of various [Retrofit] instances that serve different purposes.
 */
interface Retrofits {
    /**
     * Allows access to "/api" calls that must be authenticated.
     *
     * The base URL can be dynamically determined via the [BaseUrlInterceptors].
     */
    val authenticatedApiRetrofit: Retrofit

    /**
     * Allows access to "/api" calls that do not require authentication.
     *
     * The base URL can be dynamically determined via the [BaseUrlInterceptors].
     */
    val unauthenticatedApiRetrofit: Retrofit

    /**
     * Allows access to "/identity" calls that do not require authentication.
     *
     * The base URL can be dynamically determined via the [BaseUrlInterceptors].
     */
    val unauthenticatedIdentityRetrofit: Retrofit

    /**
     * Allows access to static API calls (ex: external APIs) that do not therefore require
     * authentication with Bitwarden's servers.
     *
     * No base URL is supplied as part of the builder and no longer is added to make this URL
     * dynamically updatable.
     */
    val staticRetrofitBuilder: Retrofit.Builder
}
