package com.bitwarden.network.interceptor

import com.bitwarden.network.provider.RefreshTokenProvider
import com.bitwarden.network.util.HEADER_KEY_AUTHORIZATION
import com.bitwarden.network.util.HEADER_VALUE_BEARER_PREFIX
import com.bitwarden.network.util.parseJwtTokenDataOrNull
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit

private const val AUTH_TOKEN_SYNC_HEADER: String = "Access-Token-Sync"
private const val MISSING_TOKEN_MESSAGE: String = "Auth token is missing!"
private const val MISSING_PROVIDER_MESSAGE: String = "Refresh token provider is missing!"
private const val EXPIRATION_OFFSET_MINUTES: Long = 5L

/**
 * Interceptor responsible for adding the auth token(Bearer) to API requests.
 */
internal class AuthTokenManager(
    private val clock: Clock,
    private val authTokenProvider: AuthTokenProvider,
) : Authenticator, Interceptor {
    var refreshTokenProvider: RefreshTokenProvider? = null

    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? {
        synchronized(this) {
            if (response.shouldSkipAuthentication()) {
                // If the same request keeps failing, let's just let the 401 pass through.
                return null
            }
            val accessToken = requireNotNull(
                response
                    .request
                    .header(name = HEADER_KEY_AUTHORIZATION)
                    ?.substringAfter(delimiter = HEADER_VALUE_BEARER_PREFIX),
            )
            return when (val userId = parseJwtTokenDataOrNull(accessToken)?.userId) {
                null -> {
                    // We are unable to get the user ID, let's just let the 401 pass through.
                    null
                }

                else -> {
                    Timber.d("Attempting to refresh token due to unauthorized")
                    refreshTokenProvider
                        ?.refreshAccessTokenSynchronously(userId = userId)
                        ?.fold(
                            onFailure = { null },
                            onSuccess = { newAccessToken ->
                                response
                                    .request
                                    .newBuilder()
                                    .header(
                                        name = HEADER_KEY_AUTHORIZATION,
                                        value = "$HEADER_VALUE_BEARER_PREFIX$newAccessToken",
                                    )
                                    .build()
                            },
                        )
                }
            }
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = synchronized(this) {
            val tokenData = authTokenProvider
                .getAuthTokenDataOrNull()
                ?: throw IOException(IllegalStateException(MISSING_TOKEN_MESSAGE))
            val expirationTime = Instant
                .ofEpochSecond(tokenData.expiresAtSec)
                .minus(EXPIRATION_OFFSET_MINUTES, ChronoUnit.MINUTES)
            if (clock.instant().isAfter(expirationTime) ||
                chain.request().header(AUTH_TOKEN_SYNC_HEADER).toBoolean()
            ) {
                Timber.d("Attempting to refresh token due to expiration")
                refreshTokenProvider
                    ?.refreshAccessTokenSynchronously(userId = tokenData.userId)
                    ?.getOrElse { throw IOException(it) }
                    ?: throw IOException(IllegalStateException(MISSING_PROVIDER_MESSAGE))
            } else {
                tokenData.accessToken
            }
        }
        val request = chain
            .request()
            .newBuilder()
            .removeHeader(AUTH_TOKEN_SYNC_HEADER)
            .addHeader(
                name = HEADER_KEY_AUTHORIZATION,
                value = "${HEADER_VALUE_BEARER_PREFIX}$token",
            )
            .build()
        return chain.proceed(request)
    }

    private fun Response.shouldSkipAuthentication(): Boolean = this.priorResponse != null
}
