package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the user decryption options received on sync.
 */
@Serializable
data class UserDecryptionJson(
    @SerialName("masterPasswordUnlock")
    val masterPasswordUnlock: MasterPasswordUnlockDataJson?,
)
