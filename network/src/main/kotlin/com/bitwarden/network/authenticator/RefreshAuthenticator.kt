package com.bitwarden.network.authenticator

import com.bitwarden.network.provider.RefreshTokenProvider
import com.bitwarden.network.util.HEADER_KEY_AUTHORIZATION
import com.bitwarden.network.util.HEADER_VALUE_BEARER_PREFIX
import com.bitwarden.network.util.parseJwtTokenDataOrNull
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber

/**
 * An authenticator used to refresh the access token when a 401 is returned from an API. Upon
 * successfully getting a new access token, the original request is retried.
 */
internal class RefreshAuthenticator : Authenticator {

    var refreshTokenProvider: RefreshTokenProvider? = null

    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? {
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

    private fun Response.shouldSkipAuthentication(): Boolean = this.priorResponse != null
}
