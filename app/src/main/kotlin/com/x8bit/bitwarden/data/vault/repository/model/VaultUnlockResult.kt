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
     * Authentication failed. This includes an incorrect password as well as other failed
     * authentication attempts, such as a biometric cipher that is missing its Keystore
     * authorization token. Callers should display a dismissible error and preserve biometric
     * eligibility so a user-initiated retry can request a fresh cipher.
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
 * Invokes the [onError] lambda as a side effect.
 */
inline fun VaultUnlockResult.onVaultUnlockError(
    onError: (VaultUnlockError) -> Unit,
): VaultUnlockResult = when (this) {
    is VaultUnlockError -> this.also { onError(this) }
    is VaultUnlockResult.Success -> this
}

/**
 * Invokes the [onSuccess] lambda as a side effect.
 */
inline fun VaultUnlockResult.onVaultUnlockSuccess(
    onSuccess: () -> Unit,
): VaultUnlockResult = when (this) {
    is VaultUnlockError -> this
    is VaultUnlockResult.Success -> this.also { onSuccess() }
}
