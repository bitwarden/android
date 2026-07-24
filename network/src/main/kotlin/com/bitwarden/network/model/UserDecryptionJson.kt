package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the user decryption options received on sync.
 *
 * @property masterPasswordUnlock The unlock data when the user has a master password that can be
 * used to decrypt their vault.
 * @property v2UpgradeToken The V2 upgrade token returned when available, allowing vault unlock
 * after V1 → V2 upgrade.
 */
@Serializable
data class UserDecryptionJson(
    @SerialName("masterPasswordUnlock")
    val masterPasswordUnlock: MasterPasswordUnlockDataJson?,

    @SerialName("v2UpgradeToken")
    val v2UpgradeToken: V2UpgradeTokenJson?,
)
