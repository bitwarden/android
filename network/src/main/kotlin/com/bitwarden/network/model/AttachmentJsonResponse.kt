package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the JSON response from creating a new attachment.
 */
sealed class AttachmentJsonResponse {
    /**
     * Represents a successful response from create attachment request.
     *
     * @property attachmentId The ID of the attachment.
     * @property url The URL of the attachment.
     * @property fileUploadType The type of file upload.
     * @property cipherResponse The cipher response associated with the attachment.
     */
    @Serializable
    data class Success(
        @SerialName("attachmentId")
        val attachmentId: String,

        @SerialName("url")
        val url: String,

        @SerialName("fileUploadType")
        val fileUploadType: FileUploadType,

        @SerialName("cipherResponse")
        val cipherResponse: SyncResponseJson.Cipher,
    ) : AttachmentJsonResponse()

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
    ) : AttachmentJsonResponse(), InvalidJsonResponse
}
