package com.x8bit.bitwarden.data.platform.datasource.network.authenticator

import com.x8bit.bitwarden.data.auth.repository.util.parseJwtTokenDataOrNull
import com.x8bit.bitwarden.data.platform.datasource.network.util.HEADER_KEY_AUTHORIZATION
import com.x8bit.bitwarden.data.platform.datasource.network.util.HEADER_VALUE_BEARER_PREFIX
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Singleton

/**
 * An authenticator used to refresh the access token when a 401 is returned from an API. Upon
 * successfully getting a new access token, the original request is retried.
 */
@Singleton
class RefreshAuthenticator : Authenticator {

    /**
     * A provider required to update tokens.
     */
    var authenticatorProvider: AuthenticatorProvider? = null

    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? {
        val accessToken = requireNotNull(
            response
                .request
                .header(name = HEADER_KEY_AUTHORIZATION)
                ?.substringAfter(delimiter = HEADER_VALUE_BEARER_PREFIX),
        )
        return when (val userId = parseJwtTokenDataOrNull(accessToken)?.userId) {
            null -> {
                // We unable to get the user ID, let's just let the 401 pass through.
                null
            }

            authenticatorProvider?.activeUserId -> {
                // In order to prevent potential deadlocks or thread starvation we want the call
                // to refresh the access token to be strictly synchronous with no internal thread
                // hopping.
                authenticatorProvider
                    ?.refreshAccessTokenSynchronously(userId)
                    ?.fold(
                        onFailure = {
                            authenticatorProvider?.logout(userId)
                            null
                        },
                        onSuccess = {
                            response.request
                                .newBuilder()
                                .header(
                                    name = HEADER_KEY_AUTHORIZATION,
                                    value = "$HEADER_VALUE_BEARER_PREFIX${it.accessToken}",
                                )
                                .build()
                        },
                    )
            }

            else -> {
                // We are no longer the active user, let's just cancel.
                null
            }
        }
    }
}
