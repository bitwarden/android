package com.x8bit.bitwarden.data.platform.datasource.network.interceptor

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
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
class AuthTokenInterceptor(
    private val authDiskSource: AuthDiskSource,
) : Interceptor {
    /**
     * The auth token to be added to API requests.
     *
     * Note: This is done on demand to ensure that no race conditions can exist when retrieving the
     * token.
     */
    private val authToken: String?
        get() = authDiskSource
            .userState
            ?.activeUserId
            ?.let { userId -> authDiskSource.getAccountTokens(userId = userId)?.accessToken }

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
