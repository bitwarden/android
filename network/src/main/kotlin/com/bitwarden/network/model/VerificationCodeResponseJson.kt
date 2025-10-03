package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Models response bodies from verification code response.
 */
@Serializable
sealed class VerificationCodeResponseJson {
    /**
     * The success body of the verification code response.
     */
    @Serializable
    data object Success : VerificationCodeResponseJson()

    /**
     * Models the json body of verification code error.
     *
     * @param message a human readable error message.
     * @param validationErrors a map where each value is a list of error messages for each key.
     * The values in the array should be used for display to the user, since the keys tend to come
     * back as nonsense. (eg: empty string key)
     */
    @Serializable
    data class Invalid(
        @SerialName("message")
        override val message: String,

        @SerialName("validationErrors")
        override val validationErrors: Map<String, List<String>>?,
    ) : VerificationCodeResponseJson(), InvalidJsonResponse
}
