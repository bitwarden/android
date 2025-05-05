package com.bitwarden.authenticator.data.auth.repository

/**
 * Provides and API for modifying authentication state.
 */
interface AuthRepository {

    /**
     * Updates the "last active time" for the current user.
     */
    fun updateLastActiveTime()
}
