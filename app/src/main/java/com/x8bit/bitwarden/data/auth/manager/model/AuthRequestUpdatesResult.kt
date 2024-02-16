package com.x8bit.bitwarden.data.auth.manager.model

/**
 * Models result of an authorization approval request.
 */
sealed class AuthRequestUpdatesResult {
    /**
     * Models the data returned when creating an auth request.
     */
    data class Update(
        val authRequest: AuthRequest,
    ) : AuthRequestUpdatesResult()

    /**
     * The auth request has been approved.
     */
    data object Approved : AuthRequestUpdatesResult()

    /**
     * There was an error getting the user's auth requests.
     */
    data object Error : AuthRequestUpdatesResult()

    /**
     * The auth request has been declined.
     */
    data object Declined : AuthRequestUpdatesResult()

    /**
     * The auth request has expired.
     */
    data object Expired : AuthRequestUpdatesResult()
}
