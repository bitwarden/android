package com.x8bit.bitwarden.data.vault.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents an update cipher collections request.
 *
 * @property collectionIds A list of collection ids associated with the cipher.
 */
@Serializable
data class UpdateCipherCollectionsJsonRequest(
    @SerialName("CollectionIds")
    val collectionIds: List<String>,
)
