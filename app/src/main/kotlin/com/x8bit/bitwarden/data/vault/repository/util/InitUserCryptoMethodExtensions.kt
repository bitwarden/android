package com.x8bit.bitwarden.data.vault.repository.util

import com.bitwarden.core.InitUserCryptoMethod

/**
 * Returns the label for the given [InitUserCryptoMethod].
 * This will be only used for logging purposes, therefore it is not localized.
 */
val InitUserCryptoMethod.logTag: String
    get() = when (this) {
        is InitUserCryptoMethod.AuthRequest -> "Auth Request"
        is InitUserCryptoMethod.DecryptedKey -> "Decrypted Key (Never Lock/Biometrics)"
        is InitUserCryptoMethod.DeviceKey -> "Device Key"
        is InitUserCryptoMethod.KeyConnector -> "Key Connector"
        is InitUserCryptoMethod.Password -> "Password"
        is InitUserCryptoMethod.Pin -> "Pin"
        is InitUserCryptoMethod.PinEnvelope -> "Pin Envelope"
        is InitUserCryptoMethod.MasterPasswordUnlock -> "Master Password Unlock"
    }
