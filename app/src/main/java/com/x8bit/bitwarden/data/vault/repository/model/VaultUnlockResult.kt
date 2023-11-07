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
    data object AuthenticationError : VaultUnlockResult()

    /**
     * Unable to access user state information.
     */
    data object InvalidStateError : VaultUnlockResult()

    /**
     * Generic error thrown by Bitwarden SDK.
     */
    data object GenericError : VaultUnlockResult()
}
