package com.x8bit.bitwarden.data.auth.datasource.disk.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Container for the user's API tokens.
 *
 * @property accessToken The user's primary access token.
 * @property refreshToken The user's refresh token.
 * @property expiresAtSec The time at which the token expires in epoch seconds.
 */
@Serializable
data class AccountTokensJson(
    @SerialName("accessToken")
    val accessToken: String?,

    @SerialName("refreshToken")
    val refreshToken: String?,

    @SerialName("expiresAtSec")
    val expiresAtSec: Long = Instant.MAX.epochSecond,
) {
    /**
     * Returns `true` if the user is logged in, `false otherwise.
     */
    val isLoggedIn: Boolean get() = accessToken != null
}
