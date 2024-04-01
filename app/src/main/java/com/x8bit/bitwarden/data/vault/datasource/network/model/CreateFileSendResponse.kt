package com.x8bit.bitwarden.data.vault.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a response from create file send.
 */
sealed class CreateFileSendResponse {

    /**
     * Represents the response from a successful create file send request.
     *
     * @property createFileJsonResponse Response JSON received from create file send request.
     */
    data class Success(
        val createFileJsonResponse: CreateFileSendResponseJson,
    ) : CreateFileSendResponse()

    /**
     * Represents the json body of an invalid create request.
     *
     * @property message A general, user-displayable error message.
     * @property validationErrors a map where each value is a list of error messages for each
     * key. The values in the array should be used for display to the user, since the keys tend
     * to come back as nonsense. (eg: empty string key)
     */
    @Serializable
    data class Invalid(
        @SerialName("message")
        override val message: String,

        @SerialName("validationErrors")
        override val validationErrors: Map<String, List<String>>?,
    ) : CreateFileSendResponse(), InvalidJsonResponse
}
