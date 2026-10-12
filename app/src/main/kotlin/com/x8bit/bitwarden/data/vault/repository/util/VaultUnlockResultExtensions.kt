package com.x8bit.bitwarden.data.vault.repository.util

import com.x8bit.bitwarden.data.platform.util.isKeystoreUserAuthenticationRequired
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.InitializeCryptoResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import java.security.GeneralSecurityException

/**
 * Transform a [InitializeCryptoResult] to [VaultUnlockResult].
 */
fun InitializeCryptoResult.toVaultUnlockResult(): VaultUnlockResult =
    when (this) {
        is InitializeCryptoResult.AuthenticationError -> {
            VaultUnlockResult.AuthenticationError(
                message = this.message,
                error = error,
            )
        }

        InitializeCryptoResult.Success -> VaultUnlockResult.Success
    }

/**
 * Maps a biometric cipher security failure to a [VaultUnlockResult].
 *
 * A Keystore authorization miss is reported separately so callers can retry before clearing
 * biometrics. All other security failures remain decoding errors.
 */
fun GeneralSecurityException.toVaultUnlockResult(): VaultUnlockResult =
    if (isKeystoreUserAuthenticationRequired()) {
        VaultUnlockResult.BiometricKeystoreAuthorizationError(error = this)
    } else {
        VaultUnlockResult.BiometricDecodingError(error = this)
    }
