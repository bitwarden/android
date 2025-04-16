package com.bitwarden.network.interceptor

import com.bitwarden.network.util.HEADER_KEY_AUTHORIZATION
import com.bitwarden.network.util.HEADER_VALUE_BEARER_PREFIX
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Singleton

/**
 * Interceptor responsible for adding the auth token(Bearer) to API requests.
 */
@Singleton
class AuthTokenInterceptor(
    private val authTokenProvider: AuthTokenProvider,
) : Interceptor {
    private val missingTokenMessage = "Auth token is missing!"

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = authTokenProvider.getActiveAccessTokenOrNull()
            ?: throw IOException(IllegalStateException(missingTokenMessage))
        val request = chain
            .request()
            .newBuilder()
            .addHeader(
                name = HEADER_KEY_AUTHORIZATION,
                value = "${HEADER_VALUE_BEARER_PREFIX}$token",
            )
            .build()
        return chain
            .proceed(request)
    }
}
