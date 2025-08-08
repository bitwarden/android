package com.bitwarden.network.interceptor

import com.bitwarden.network.model.AuthTokenData

/**
 * A provider for all the functionality needed to properly refresh the users access token.
 */
interface AuthTokenProvider {

    /**
     * The currently active user's auth token data.
     */
    fun getAuthTokenDataOrNull(): AuthTokenData?
}
