package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a request to create an attachment.
 */
@Serializable
data class AttachmentJsonRequest(
    @SerialName("fileName")
    val fileName: String?,

    @SerialName("key")
    val key: String?,

    @SerialName("fileSize")
    val fileSize: String?,
)
