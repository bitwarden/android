package com.x8bit.bitwarden.data.auth.repository.model

import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockError
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult

/**
 * Helper function to map a [VaultUnlockError] to a [LoginResult.Error] with
 * the necessary `message` if applicable.
 */
fun VaultUnlockError.toLoginErrorResult(): LoginResult.Error = when (this) {
    is VaultUnlockResult.AuthenticationError -> {
        LoginResult.Error(errorMessage = this.message, error = this.error)
    }

    is VaultUnlockResult.BiometricDecodingError,
    is VaultUnlockResult.GenericError,
    is VaultUnlockResult.InvalidStateError,
        -> LoginResult.Error(errorMessage = null, error = this.error)
}
