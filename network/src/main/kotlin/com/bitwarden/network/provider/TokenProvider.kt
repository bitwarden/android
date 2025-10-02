package com.bitwarden.network.provider

/**
 * A provider for authentication tokens.
 */
interface TokenProvider {
    /**
     * Retrieves an up-to-date token for the specified user.
     */
    fun getAccessToken(userId: String): String?
}
