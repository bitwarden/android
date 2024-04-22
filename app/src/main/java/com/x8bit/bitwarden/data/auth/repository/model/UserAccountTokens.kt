package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Associates the [accessToken] and [refreshToken] with the given [userId].
 */
data class UserAccountTokens(
    val userId: String,
    val accessToken: String?,
    val refreshToken: String?,
) {
    /**
     * Returns `true` if the user is logged in, `false otherwise.
     */
    val isLoggedIn: Boolean get() = accessToken != null
}
