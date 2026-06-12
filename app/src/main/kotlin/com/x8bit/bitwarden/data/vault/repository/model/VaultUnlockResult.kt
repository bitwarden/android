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
        override val error: Throwable?,
    ) : VaultUnlockResult(), VaultUnlockError

    /**
     * Unable to decode biometrics key.
     */
    data class BiometricDecodingError(
        override val error: Throwable?,
    ) : VaultUnlockResult(), VaultUnlockError

    /**
     * Unable to access user state information.
     */
    data class InvalidStateError(
        override val error: Throwable?,
    ) : VaultUnlockResult(), VaultUnlockError

    /**
     * Generic error thrown by Bitwarden SDK.
     */
    data class GenericError(
        override val error: Throwable?,
    ) : VaultUnlockResult(), VaultUnlockError
}

/**
 * Sealed interface to denote that a [VaultUnlockResult] is an error result.
 */
sealed interface VaultUnlockError {
    val error: Throwable?
}

/**
 * Invokes the [onSuccess] lambda as a side effect.
 */
inline fun VaultUnlockResult.onVaultUnlockSuccess(
    onSuccess: () -> Unit,
): VaultUnlockResult = when (this) {
    is VaultUnlockResult.AuthenticationError,
    is VaultUnlockResult.BiometricDecodingError,
    is VaultUnlockResult.GenericError,
    is VaultUnlockResult.InvalidStateError,
        -> this

    is VaultUnlockResult.Success -> this.also { onSuccess() }
}
