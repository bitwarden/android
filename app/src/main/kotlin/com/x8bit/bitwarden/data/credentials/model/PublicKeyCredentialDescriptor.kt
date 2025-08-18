package com.x8bit.bitwarden.data.credentials.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents details about a credential provided in the creation options.
 */
@Serializable
data class PublicKeyCredentialDescriptor(
    @SerialName("type")
    val type: String,
    @SerialName("id")
    val id: String,
    @SerialName("transports")
    val transports: List<String>?,
)
