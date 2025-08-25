package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the JSON response from refreshing the access token.
 */
sealed class RefreshTokenResponseJson {
    /**
     * Models a successful response body from the refresh token request.
     *
     * @property accessToken The new access token.
     * @property expiresIn When the new [accessToken] expires.
     * @property refreshToken The new refresh token.
     * @property tokenType The type of token the new [accessToken] is.
     */
    @Serializable
    data class Success(
        @SerialName("access_token")
        val accessToken: String,

        @SerialName("expires_in")
        val expiresIn: Int,

        @SerialName("refresh_token")
        val refreshToken: String,

        @SerialName("token_type")
        val tokenType: String,
    ) : RefreshTokenResponseJson()

    /**
     * Models a failure response body from the refresh token request.
     */
    @Serializable
    data class Error(
        @SerialName("error")
        val error: String,
    ) : RefreshTokenResponseJson() {
        val isInvalidGrant: Boolean get() = error == "invalid_grant"
    }

    /**
     * Models a failure response with a 403 "Forbidden" response code.
     */
    data class Forbidden(
        val error: Throwable,
    ) : RefreshTokenResponseJson()

    /**
     * Models a failure response with a 401 "Unauthorized" response code.
     */
    data class Unauthorized(
        val error: Throwable,
    ) : RefreshTokenResponseJson()
}
