package com.bitwarden.network.interceptor

/**
 * A provider for all the functionality needed to properly refresh the users access token.
 */
interface AuthTokenProvider {

    /**
     * The currently active user's access token.
     */
    fun getActiveAccessTokenOrNull(): String?
}
