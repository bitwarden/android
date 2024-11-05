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
    @SerialName("accessToken")
    val accessToken: String,

    @SerialName("expiresIn")
    val expiresIn: Int,

    @SerialName("refreshToken")
    val refreshToken: String,

    @SerialName("tokenType")
    val tokenType: String,
)
