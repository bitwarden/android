package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Models the response body from the refresh token request.
 *
 * @property accessToken The new access token.
 * @property expiresIn When the new [accessToken] expires.
 * @property refreshToken The new refresh token.
 * @property tokenType The type of token the new [accessToken] is.
 */
@Serializable
data class RefreshTokenResponseJson(
    @SerialName("access_token")
    val accessToken: String,

    @SerialName("expires_in")
    val expiresIn: Int,

    @SerialName("refresh_token")
    val refreshToken: String,

    @SerialName("token_type")
    val tokenType: String,
)
