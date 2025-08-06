package com.bitwarden.network.interceptor

import com.bitwarden.network.provider.RefreshTokenProvider
import com.bitwarden.network.util.HEADER_KEY_AUTHORIZATION
import com.bitwarden.network.util.HEADER_VALUE_BEARER_PREFIX
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit

private const val MISSING_TOKEN_MESSAGE: String = "Auth token is missing!"
private const val MISSING_PROVIDER_MESSAGE: String = "Refresh token provider is missing!"
private const val EXPIRATION_OFFSET_MINUTES: Long = 5L

/**
 * Interceptor responsible for adding the auth token(Bearer) to API requests.
 */
internal class AuthTokenInterceptor(
    private val clock: Clock,
    private val authTokenProvider: AuthTokenProvider,
) : Interceptor {
    var refreshTokenProvider: RefreshTokenProvider? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val tokenData = authTokenProvider
            .getAuthTokenDataOrNull()
            ?: throw IOException(IllegalStateException(MISSING_TOKEN_MESSAGE))
        val expirationTime = Instant
            .ofEpochSecond(tokenData.expiresAtSec)
            .minus(EXPIRATION_OFFSET_MINUTES, ChronoUnit.MINUTES)
        val token = if (clock.instant().isAfter(expirationTime)) {
            Timber.d("Attempting to refresh token due to expiration")
            refreshTokenProvider
                ?.refreshAccessTokenSynchronously(userId = tokenData.userId)
                ?.getOrElse { throw IOException(it) }
                ?: throw IOException(IllegalStateException(MISSING_PROVIDER_MESSAGE))
        } else {
            tokenData.accessToken
        }
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
