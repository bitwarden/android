package com.x8bit.bitwarden.data.auth.datasource.disk.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Container for the user's API tokens.
 *
 * @property accessToken The user's primary access token.
 * @property refreshToken The user's refresh token.
 */
@Serializable
data class AccountTokensJson(
    @SerialName("accessToken")
    val accessToken: String?,

    @SerialName("refreshToken")
    val refreshToken: String?,
) {
    /**
     * Returns `true` if the user is logged in, `false otherwise.
     */
    val isLoggedIn: Boolean get() = accessToken != null
}
