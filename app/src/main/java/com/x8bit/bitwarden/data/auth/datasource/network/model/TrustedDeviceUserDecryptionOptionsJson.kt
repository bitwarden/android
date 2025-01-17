package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

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
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class TrustedDeviceUserDecryptionOptionsJson(
    @SerialName("encryptedPrivateKey")
    @JsonNames("EncryptedPrivateKey")
    val encryptedPrivateKey: String?,

    @SerialName("encryptedUserKey")
    @JsonNames("EncryptedUserKey")
    val encryptedUserKey: String?,

    @SerialName("hasAdminApproval")
    @JsonNames("HasAdminApproval")
    val hasAdminApproval: Boolean,

    @SerialName("hasLoginApprovingDevice")
    @JsonNames("HasLoginApprovingDevice")
    val hasLoginApprovingDevice: Boolean,

    @SerialName("hasManageResetPasswordPermission")
    @JsonNames("HasManageResetPasswordPermission")
    val hasManageResetPasswordPermission: Boolean,
)
