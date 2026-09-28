package com.x8bit.bitwarden.data.platform.manager.keyrotation

import kotlinx.coroutines.flow.Flow

/**
 * A manager for rotating user keys stored to disk.
 */
interface KeyRotationManager {
    /**
     * Emits when the user needs to be prompted to rotate the Biometrics Key.
     */
    val shouldRotateBiometricKey: Flow<Unit>

    /**
     * Checks if the Authenticator Sync key needs to be rotated ands does so if needed.
     */
    suspend fun rotateAuthenticatorSyncKey(userId: String)

    /**
     * Checks if the Auto-unlock key needs to be rotated ands does so if needed.
     */
    suspend fun rotateAutoUnlockKey(userId: String)

    /**
     * Checks if the Biometrics key needs to be rotated ands prompts the user to do so if needed.
     */
    suspend fun rotateBiometricsKey(userId: String, decryptedBiometricsUserKey: String)
}
