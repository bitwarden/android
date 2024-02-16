package com.x8bit.bitwarden.data.auth.manager.model

/**
 * Models result of getting the list of login approval requests for the current user.
 */
sealed class AuthRequestsResult {
    /**
     * Models the result of getting a user's auth requests.
     */
    data class Success(
        val authRequests: List<AuthRequest>,
    ) : AuthRequestsResult()

    /**
     * There was an error getting the user's auth requests.
     */
    data object Error : AuthRequestsResult()
}
