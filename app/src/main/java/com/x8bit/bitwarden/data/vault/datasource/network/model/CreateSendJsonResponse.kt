package com.x8bit.bitwarden.data.vault.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Models a response from either "create text send" or "create file send" requests.
 */
sealed class CreateSendJsonResponse {
    /**
     * Represents a successful response from either "create text send" or "create file send"
     * request.
     *
     * @property send The created send object.
     */
    data class Success(val send: SyncResponseJson.Send) : CreateSendJsonResponse()

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
    ) : CreateSendJsonResponse(), InvalidJsonResponse
}
