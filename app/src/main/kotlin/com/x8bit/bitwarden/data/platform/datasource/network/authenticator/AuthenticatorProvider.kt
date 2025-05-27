package com.x8bit.bitwarden.data.platform.datasource.network.authenticator

import com.bitwarden.network.provider.RefreshTokenProvider
import com.x8bit.bitwarden.data.auth.repository.model.LogoutReason

/**
 * A provider for all the functionality needed to properly refresh the users access token.
 */
interface AuthenticatorProvider : RefreshTokenProvider {

    /**
     * The currently active user's ID.
     */
    val activeUserId: String?

    /**
     * Attempts to logout the user based on the [userId].
     */
    fun logout(userId: String, reason: LogoutReason)
}
