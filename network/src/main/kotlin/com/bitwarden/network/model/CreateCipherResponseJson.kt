package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Models the response from the create cipher request.
 */
sealed class CreateCipherResponseJson {

    /**
     * The request completed successfully and returned the created [cipher].
     */
    data class Success(val cipher: SyncResponseJson.Cipher) : CreateCipherResponseJson()

    /**
     * Represents the json body of an invalid create request.
     *
     * @param message A general, user-displayable error message.
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
    ) : CreateCipherResponseJson(), InvalidJsonResponse
}
