package com.x8bit.bitwarden.data.auth.manager.model

/**
 * Models result of creating a new login approval request.
 */
sealed class AuthRequestResult {
    /**
     * Models the data returned when creating an auth request.
     */
    data class Success(
        val authRequest: AuthRequest,
    ) : AuthRequestResult()

    /**
     * There was an error getting the user's auth requests.
     */
    data object Error : AuthRequestResult()
}
