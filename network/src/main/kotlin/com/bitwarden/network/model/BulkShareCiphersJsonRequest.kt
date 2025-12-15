package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a bulk share ciphers request.
 *
 * @property ciphers The list of ciphers to share.
 * @property collectionIds A list of collection IDs to associate with all ciphers.
 */
@Serializable
data class BulkShareCiphersJsonRequest(
    @SerialName("Ciphers")
    val ciphers: List<CipherJsonRequest>,

    @SerialName("CollectionIds")
    val collectionIds: List<String>,
)
