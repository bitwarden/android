package com.x8bit.bitwarden.data.vault.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a create cipher in organization request.
 *
 * @property cipher The cipher to create.
 * @property collectionIds A list of collection ids associated with the cipher.
 */
@Serializable
data class CreateCipherInOrganizationJsonRequest(
    @SerialName("Cipher")
    val cipher: CipherJsonRequest,

    @SerialName("CollectionIds")
    val collectionIds: List<String>,
)
