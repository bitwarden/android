package com.x8bit.bitwarden.data.vault.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the JSON response from creating a new file send.
 */
@Serializable
data class CreateFileSendResponseJson(
    @SerialName("url")
    val url: String,

    @SerialName("fileUploadType")
    val fileUploadType: FileUploadType,

    @SerialName("sendResponse")
    val sendResponse: SyncResponseJson.Send,
)
