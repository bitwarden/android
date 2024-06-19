package com.x8bit.bitwarden.data.vault.datasource.network.model

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
