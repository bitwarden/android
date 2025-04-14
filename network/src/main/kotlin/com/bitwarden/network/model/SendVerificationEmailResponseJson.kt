package com.bitwarden.network.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * The response body for sending a verification email.
 */
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
     * @param validationErrors a map where each value is a list of error messages for each key.
     * The values in the array should be used for display to the user, since the keys tend to come
     * back as nonsense. (eg: empty string key)
     */
    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    data class Invalid(
        @JsonNames("message")
        @SerialName("Message")
        private val errorMessage: String? = null,

        @SerialName("validationErrors")
        private val validationErrors: Map<String, List<String>>?,
    ) : SendVerificationEmailResponseJson() {
        /**
         * A generic error message.
         */
        val message: String?
            get() = validationErrors
                ?.values
                ?.firstOrNull()
                ?.firstOrNull()
                ?: errorMessage
    }
}
