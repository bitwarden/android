package com.bitwarden.authenticator.data.auth.repository

import com.bitwarden.authenticator.data.platform.manager.BiometricsEncryptionManager
import com.bitwarden.authenticator.data.platform.repository.model.BiometricsKeyResult
import com.bitwarden.authenticator.data.platform.repository.model.BiometricsUnlockResult
import kotlinx.coroutines.flow.StateFlow
import javax.crypto.Cipher

/**
 * Provides and API for modifying authentication state.
 */
interface AuthRepository : BiometricsEncryptionManager {

    /**
     * Whether or not biometric unlocking is enabled for the current user.
     */
    val isUnlockWithBiometricsEnabled: Boolean

    /**
     * Tracks whether or not biometric unlocking is enabled for the current user.
     */
    val isUnlockWithBiometricsEnabledFlow: StateFlow<Boolean>

    /**
     * Stores the encrypted user key for biometrics, allowing it to be used to unlock the current
     * user's vault.
     */
    suspend fun setupBiometricsKey(cipher: Cipher): BiometricsKeyResult

    /**
     * Attempt to unlock the vault using the stored biometric key.
     */
    suspend fun unlockWithBiometrics(cipher: Cipher): BiometricsUnlockResult

    /**
     * Updates the "last active time" for the current user.
     */
    fun updateLastActiveTime()
}
