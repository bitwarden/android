package com.x8bit.bitwarden.data.vault.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Models the responses from the folder update request.
 */
sealed class UpdateFolderResponseJson {
    /**
     * The request completed successfully and returned the updated [folder].
     */
    data class Success(
        val folder: SyncResponseJson.Folder,
    ) : UpdateFolderResponseJson()

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
    ) : UpdateFolderResponseJson()
}
