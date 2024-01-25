package com.x8bit.bitwarden.data.vault.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the JSON response from creating a new attachment.
 */
@Serializable
data class AttachmentJsonResponse(
    @SerialName("attachmentId")
    val attachmentId: String,

    @SerialName("url")
    val url: String,

    @SerialName("fileUploadType")
    val fileUploadType: FileUploadType,

    @SerialName("cipherResponse")
    val cipherResponse: SyncResponseJson.Cipher,
)
