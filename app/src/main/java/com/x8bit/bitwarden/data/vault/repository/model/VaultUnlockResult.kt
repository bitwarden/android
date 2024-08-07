package com.x8bit.bitwarden.data.vault.repository.model

/**
 * Models result of unlocking the vault.
 */
sealed class VaultUnlockResult {

    /**
     * Vault successfully unlocked.
     */
    data object Success : VaultUnlockResult()

    /**
     * Incorrect password provided.
     */
    data class AuthenticationError(
        val message: String? = null,
    ) : VaultUnlockResult(), VaultUnlockError

    /**
     * Unable to access user state information.
     */
    data object InvalidStateError : VaultUnlockResult(), VaultUnlockError

    /**
     * Generic error thrown by Bitwarden SDK.
     */
    data object GenericError : VaultUnlockResult(), VaultUnlockError
}

/**
 * Sealed interface to denote that a [VaultUnlockResult] is an error result.
 */
sealed interface VaultUnlockError
