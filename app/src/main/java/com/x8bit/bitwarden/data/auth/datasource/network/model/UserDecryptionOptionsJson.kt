package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The options available to a user for decryption.
 *
 * @property hasMasterPassword Whether the current user has a master password that can be used to
 * decrypt their vault.
 * @property trustedDeviceUserDecryptionOptions Decryption options related to a user's trusted
 * device.
 * @property keyConnectorUserDecryptionOptions Decryption options related to a user's key connector.
 */
@Serializable
data class UserDecryptionOptionsJson(
    @SerialName("hasMasterPassword")
    val hasMasterPassword: Boolean,

    @SerialName("trustedDeviceOption")
    val trustedDeviceUserDecryptionOptions: TrustedDeviceUserDecryptionOptionsJson?,

    @SerialName("keyConnectorOption")
    val keyConnectorUserDecryptionOptions: KeyConnectorUserDecryptionOptionsJson?,
)
