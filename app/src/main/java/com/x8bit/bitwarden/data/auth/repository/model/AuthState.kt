package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Models high level auth state for the application.
 */
sealed class AuthState {

    /**
     * Auth state is unknown.
     */
    data object Uninitialized : AuthState()

    /**
     * User is unauthenticated. Said another way, the app has no access token.
     */
    data object Unauthenticated : AuthState()

    /**
     * User is authenticated with the given access token.
     */
    data class Authenticated(val accessToken: String) : AuthState()
}
