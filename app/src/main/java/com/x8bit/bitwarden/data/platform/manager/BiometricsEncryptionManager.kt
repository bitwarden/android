package com.x8bit.bitwarden.data.platform.manager

import javax.crypto.Cipher

/**
 * Responsible for managing Android keystore encryption and decryption.
 */
interface BiometricsEncryptionManager {
    /**
     * Creates a [Cipher] built from a keystore.
     */
    fun createCipherOrNull(
        userId: String,
    ): Cipher?

    /**
     * Gets the [Cipher] built from a keystore, or creates one if it doesn't already exist.
     */
    fun getOrCreateCipher(
        userId: String,
    ): Cipher?

    /**
     * Sets up biometrics to ensure future integrity checks work properly. If this method has never
     * been called [isBiometricIntegrityValid] will return false.
     */
    fun setupBiometrics(userId: String)

    /**
     * Checks to verify that the biometrics integrity is still valid. This returns `true` if the
     * biometrics data has not changed since the app setup biometrics; `false` will be returned if
     * it has changed.
     */
    fun isBiometricIntegrityValid(
        userId: String,
        cipher: Cipher?,
    ): Boolean

    /**
     * Returns a boolean indicating whether the system reflects biometric availability.
     */
    fun isAccountBiometricIntegrityValid(userId: String): Boolean
}
