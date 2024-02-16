package com.x8bit.bitwarden.data.auth.manager.model

/**
 * Models result of an authorization approval request.
 */
sealed class AuthRequestsUpdatesResult {
    /**
     * Models the data returned when creating an auth request.
     */
    data class Update(
        val authRequests: List<AuthRequest>,
    ) : AuthRequestsUpdatesResult()

    /**
     * There was an error getting the user's auth requests.
     */
    data object Error : AuthRequestsUpdatesResult()
}
