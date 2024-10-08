package com.x8bit.bitwarden.data.auth.repository.model

import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockError
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult

/**
 * Helper function to map a [VaultUnlockError] to a [LoginResult.Error] with
 * the necessary `message` if applicable.
 */
fun VaultUnlockError.toLoginErrorResult(): LoginResult.Error = when (this) {
    is VaultUnlockResult.AuthenticationError -> LoginResult.Error(this.message)
    VaultUnlockResult.GenericError,
    VaultUnlockResult.InvalidStateError,
        -> LoginResult.Error(errorMessage = null)
}
