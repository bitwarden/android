package com.bitwarden.authenticator.data.platform.manager

/**
 * Responsible for managing Android keystore encryption and decryption.
 */
interface BiometricsEncryptionManager {
    /**
     * Sets up biometrics to ensure future integrity checks work properly. If this method has never
     * been called [isBiometricIntegrityValid] will return false.
     */
    fun setupBiometrics()

    /**
     * Checks to verify that the biometrics integrity is still valid. This returns `true` if the
     * biometrics data has not change since the app setup biometrics, `false` will be returned if
     * it has changed.
     */
    fun isBiometricIntegrityValid(): Boolean
}
