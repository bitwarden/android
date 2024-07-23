package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class SendVerificationEmailResponseJson {

    /**
     * Models a successful json response.
     *
     * @param emailVerificationToken the token to verify the email.
     */
    @Serializable
    data class Success(
        val emailVerificationToken: String?,
    ) : SendVerificationEmailResponseJson()

    /**
     * Represents the json body of an invalid request.
     *
     * @param message
     * @param validationErrors a map where each value is a list of error messages for each key.
     * The values in the array should be used for display to the user, since the keys tend to come
     * back as nonsense. (eg: empty string key)
     */
    @Serializable
    data class Invalid(
        @SerialName("message")
        val message: String?,

        @SerialName("validationErrors")
        val validationErrors: Map<String, List<String>>?,
    ) : SendVerificationEmailResponseJson()

    /**
     * A different error with a message.
     */
    @Serializable
    data class Error(
        @SerialName("Message")
        val message: String?,
    ) : SendVerificationEmailResponseJson()
}