package com.bitwarden.authenticator.data.platform.repository.model

/**
 * Models result of setting up a biometrics key.
 */
sealed class BiometricsKeyResult {
    /**
     * Biometrics key setup successfully.
     */
    data object Success : BiometricsKeyResult()

    /**
     * Generic error while setting up the biometrics key.
     */
    data object Error : BiometricsKeyResult()
}
