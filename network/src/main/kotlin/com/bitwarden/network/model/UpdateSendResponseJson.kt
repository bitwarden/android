package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Models the response from the update send request.
 */
sealed class UpdateSendResponseJson {
    /**
     * The request completed successfully and returned the updated [send].
     */
    data class Success(
        val send: SyncResponseJson.Send,
    ) : UpdateSendResponseJson()

    /**
     * Represents the json body of an invalid update request.
     *
     * @param message A general, user-displayable error message.
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
    ) : UpdateSendResponseJson()
}
