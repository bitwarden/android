package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for deleting an account.
 *
 * @param masterPasswordHash the master password (encrypted).
 */
@Serializable
data class DeleteAccountRequestJson(
    @SerialName("MasterPasswordHash")
    val masterPasswordHash: String,
)
