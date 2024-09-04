package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Models response bodies for the verification of the email token.
 */
@Serializable
sealed class VerifyEmailTokenResponseJson {
    /**
     * Models a successful json response of the verify email request.
     */
    @Serializable
    data object Success : VerifyEmailTokenResponseJson()

    /**
     * Represents the json body of an invalid register request.
     *
     * @param validationErrors a map where each value is a list of error messages for each key.
     * The values in the array should be used for display to the user, since the keys tend to come
     * back as nonsense. (eg: empty string key)
     */
    @Serializable
    data class Invalid(
        @SerialName("message")
        private val invalidMessage: String? = null,

        @SerialName("Message")
        private val errorMessage: String? = null,

        @SerialName("validationErrors")
        val validationErrors: Map<String, List<String>>?,
    ) : VerifyEmailTokenResponseJson() {
        /**
         * A generic error message.
         */
        val message: String? get() = invalidMessage ?: errorMessage
    }
}
