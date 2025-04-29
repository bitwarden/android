package com.bitwarden.network.provider

import com.bitwarden.network.model.RefreshTokenResponseJson

/**
 * A provider for all the functionality needed to refresh a user's access token.
 */
interface RefreshTokenProvider {
    /**
     * Attempt to refresh the user's access token based on the [userId].
     *
     * This call is both synchronous and performs a network request. Make sure that you are calling
     * from an appropriate thread.
     */
    fun refreshAccessTokenSynchronously(userId: String): Result<RefreshTokenResponseJson>
}
