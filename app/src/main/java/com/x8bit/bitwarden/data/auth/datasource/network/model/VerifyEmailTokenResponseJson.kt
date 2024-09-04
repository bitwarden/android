package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Model the response of a verify email token request.
 *
 * A valid response will be a [VerifyEmailTokenResponseJson.Valid]
 *
 * an invalid response will be a [VerifyEmailTokenResponseJson.Invalid] with a message.
 */
@Serializable
sealed class VerifyEmailTokenResponseJson {

    /**
     * The token is confirmed as valid from the response.
     */
    @Serializable
    data object Valid : VerifyEmailTokenResponseJson()

    /**
     * The response is invalid.
     *
     * @property message The error message. Expected to explain the reason why the token is invalid.
     */
    @Serializable
    data class Invalid(
        @SerialName("message")
        val message: String,
    ) : VerifyEmailTokenResponseJson()

    /**
     * The token has expired. This is special case of similar to [Invalid].
     */
    @Serializable
    data object TokenExpired : VerifyEmailTokenResponseJson()
}
