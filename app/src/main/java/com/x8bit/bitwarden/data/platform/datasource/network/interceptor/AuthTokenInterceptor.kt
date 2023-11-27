package com.x8bit.bitwarden.data.platform.datasource.network.interceptor

import com.x8bit.bitwarden.data.platform.datasource.network.util.HEADER_KEY_AUTHORIZATION
import com.x8bit.bitwarden.data.platform.datasource.network.util.HEADER_VALUE_BEARER_PREFIX
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Singleton

/**
 * Interceptor responsible for adding the auth token(Bearer) to API requests.
 */
@Singleton
class AuthTokenInterceptor : Interceptor {
    /**
     * The auth token to be added to API requests.
     */
    var authToken: String? = null

    private val missingTokenMessage = "Auth token is missing!"

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = authToken ?: throw IOException(IllegalStateException(missingTokenMessage))
        val request = chain
            .request()
            .newBuilder()
            .addHeader(
                name = HEADER_KEY_AUTHORIZATION,
                value = "$HEADER_VALUE_BEARER_PREFIX$token",
            )
            .build()
        return chain
            .proceed(request)
    }
}
