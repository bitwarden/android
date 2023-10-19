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
    @SerialName("EncryptedPrivateKey")
    val encryptedPrivateKey: String?,

    @SerialName("EncryptedUserKey")
    val encryptedUserKey: String?,

    @SerialName("HasAdminApproval")
    val hasAdminApproval: Boolean,

    @SerialName("HasLoginApprovingDevice")
    val hasLoginApprovingDevice: Boolean,

    @SerialName("HasManageResetPasswordPermission")
    val hasManageResetPasswordPermission: Boolean,
)
