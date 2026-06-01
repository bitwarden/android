package com.bitwarden.authenticator.data.platform.manager

import javax.crypto.Cipher

/**
 * Responsible for managing Android keystore encryption and decryption.
 */
interface BiometricsEncryptionManager {
    /**
     * Creates a [Cipher] built from a keystore.
     */
    fun createCipherOrNull(): Cipher?

    /**
     * Clears the data associated with the users biometrics.
     */
    fun clearBiometrics()

    /**
     * Gets the [Cipher] built from a keystore, or creates one if it doesn't already exist.
     */
    fun getOrCreateCipher(): Cipher?

    /**
     * Checks to verify that the biometrics integrity is still valid. This returns `true` if the
     * biometrics data has not changed since the app setup biometrics; `false` will be returned if
     * it has changed.
     */
    fun isBiometricIntegrityValid(): Boolean

    /**
     * Returns a boolean indicating whether the system reflects biometric availability.
     */
    fun isAccountBiometricIntegrityValid(): Boolean
}
