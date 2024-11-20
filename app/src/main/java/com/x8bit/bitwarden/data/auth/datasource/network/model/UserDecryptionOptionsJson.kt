package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * The options available to a user for decryption.
 *
 * @property hasMasterPassword Whether the current user has a master password that can be used to
 * decrypt their vault.
 * @property trustedDeviceUserDecryptionOptions Decryption options related to a user's trusted
 * device.
 * @property keyConnectorUserDecryptionOptions Decryption options related to a user's key connector.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class UserDecryptionOptionsJson(
    @SerialName("hasMasterPassword")
    @JsonNames("HasMasterPassword")
    val hasMasterPassword: Boolean,

    @SerialName("trustedDeviceOption")
    @JsonNames("TrustedDeviceOption")
    val trustedDeviceUserDecryptionOptions: TrustedDeviceUserDecryptionOptionsJson?,

    @SerialName("keyConnectorOption")
    @JsonNames("KeyConnectorOption")
    val keyConnectorUserDecryptionOptions: KeyConnectorUserDecryptionOptionsJson?,
)
