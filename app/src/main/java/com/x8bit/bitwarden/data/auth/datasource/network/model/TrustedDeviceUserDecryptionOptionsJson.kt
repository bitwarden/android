package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Decryption options related to a user's trusted device.
 *
 * @property encryptedPrivateKey The user's encrypted private key.
 * @property encryptedUserKey The user's encrypted key.
 * @property hasAdminApproval Whether or not the user has admin approval.
 * @property hasLoginApprovingDevice Whether or not the user has a login approving device.
 * @property hasManageResetPasswordPermission Whether or not the user has manage reset password
 * permission.
 */
@Serializable
data class TrustedDeviceUserDecryptionOptionsJson(
    @SerialName("encryptedPrivateKey")
    val encryptedPrivateKey: String?,

    @SerialName("encryptedUserKey")
    val encryptedUserKey: String?,

    @SerialName("hasAdminApproval")
    val hasAdminApproval: Boolean,

    @SerialName("hasLoginApprovingDevice")
    val hasLoginApprovingDevice: Boolean,

    @SerialName("hasManageResetPasswordPermission")
    val hasManageResetPasswordPermission: Boolean,
)
