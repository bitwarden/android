package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for deleting an account.
 *
 * @param masterPasswordHash The master password (encrypted).
 * @param oneTimePassword The one time password.
 */
@Serializable
data class DeleteAccountRequestJson(
    @SerialName("MasterPasswordHash")
    val masterPasswordHash: String?,
    @SerialName("otp")
    val oneTimePassword: String?,
)
