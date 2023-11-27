package com.x8bit.bitwarden.data.platform.datasource.network.authenticator

import com.x8bit.bitwarden.data.auth.datasource.network.model.RefreshTokenResponseJson

/**
 * A provider for all the functionality needed to properly refresh the users access token.
 */
interface AuthenticatorProvider {

    /**
     * The currently active user's ID.
     */
    val activeUserId: String?

    /**
     * Attempts to logout the user based on the [userId].
     */
    fun logout(userId: String)

    /**
     * Attempt to refresh the user's access token based on the [userId].
     *
     * This call is both synchronous and performs a network request. Make sure that you are calling
     * from an appropriate thread.
     */
    fun refreshAccessTokenSynchronously(userId: String): Result<RefreshTokenResponseJson>
}
