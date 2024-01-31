package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for resetting the password.
 *
 * @param currentPasswordHash The hash of the user's current password.
 * @param newPasswordHash The hash of the user's new password.
 * @param passwordHint The hint for the master password (nullable).
 * @param key The user key for the request (encrypted).
 */
@Serializable
data class ResetPasswordRequestJson(
    @SerialName("masterPasswordHash")
    val currentPasswordHash: String?,

    @SerialName("newMasterPasswordHash")
    val newPasswordHash: String,

    @SerialName("masterPasswordHint")
    val passwordHint: String?,

    @SerialName("key")
    val key: String,
)
