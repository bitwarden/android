package com.x8bit.bitwarden.data.vault.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a share cipher request.
 *
 * @property cipher The cipher to share.
 * @property collectionIds A list of collection ids associated with the cipher.
 */
@Serializable
data class ShareCipherJsonRequest(
    @SerialName("Cipher")
    val cipher: CipherJsonRequest,

    @SerialName("CollectionIds")
    val collectionIds: List<String>,
)
