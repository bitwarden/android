package com.bitwarden.authenticator.data.platform.repository.model

/**
 * Models result of unlocking the app.
 */
sealed class BiometricsUnlockResult {

    /**
     * Vault successfully unlocked.
     */
    data object Success : BiometricsUnlockResult()

    /**
     * Unable to decode biometrics key.
     */
    data class BiometricDecodingError(
        val error: Throwable?,
    ) : BiometricsUnlockResult()

    /**
     * Unable to access user state information.
     */
    data class InvalidStateError(
        val error: Throwable?,
    ) : BiometricsUnlockResult()
}
