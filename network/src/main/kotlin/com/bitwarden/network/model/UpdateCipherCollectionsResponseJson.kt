package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents API response model for updating a cipher's collections.
 *
 * @property cipher The updated cipher, or `null` if the user no longer has access.
 */
@Serializable
data class UpdateCipherCollectionsResponseJson(
    @SerialName("cipher")
    val cipher: SyncResponseJson.Cipher?,
)
