package com.bitwarden.authenticator.data.auth.repository

import com.bitwarden.authenticator.data.auth.datasource.disk.AuthDiskSource
import com.bitwarden.authenticator.data.authenticator.datasource.sdk.AuthenticatorSdkSource
import com.bitwarden.authenticator.data.platform.manager.BiometricsEncryptionManager
import com.bitwarden.authenticator.data.platform.manager.lock.AppLockManager
import com.bitwarden.authenticator.data.platform.manager.lock.model.AppLockState
import com.bitwarden.authenticator.data.platform.repository.model.BiometricsKeyResult
import com.bitwarden.authenticator.data.platform.repository.model.BiometricsUnlockResult
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.manager.realtime.RealtimeManager
import com.bitwarden.core.data.repository.error.MissingPropertyException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.inject.Inject

/**
 * Default implementation of [AuthRepository].
 */
class AuthRepositoryImpl @Inject constructor(
    private val authDiskSource: AuthDiskSource,
    private val authenticatorSdkSource: AuthenticatorSdkSource,
    private val biometricsEncryptionManager: BiometricsEncryptionManager,
    private val realtimeManager: RealtimeManager,
    private val appLockManager: AppLockManager,
    dispatcherManager: DispatcherManager,
) : AuthRepository,
    BiometricsEncryptionManager by biometricsEncryptionManager {
    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    override val isUnlockWithBiometricsEnabled: Boolean
        get() = authDiskSource.getUserBiometricUnlockKey() != null

    override val isUnlockWithBiometricsEnabledFlow: StateFlow<Boolean>
        get() = authDiskSource
            .userBiometricUnlockKeyFlow
            .map { it != null }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = isUnlockWithBiometricsEnabled,
            )

    override val appLockStateFlow: StateFlow<AppLockState> = appLockManager.appLockStateFlow

    override suspend fun setupBiometricsKey(cipher: Cipher): BiometricsKeyResult =
        authenticatorSdkSource
            .generateBiometricsKey()
            .onSuccess { biometricsKey ->
                val encryptedBiometricsKey = try {
                    cipher
                        .doFinal(biometricsKey.encodeToByteArray())
                        .toString(Charsets.ISO_8859_1)
                } catch (e: GeneralSecurityException) {
                    Timber.w(e, "setupBiometricsKey failed encrypt the biometric key")
                    return BiometricsKeyResult.Error(error = e)
                }

                // Set app to unlocked to ensure we do not re-lock after saving the biometric key.
                appLockManager.manualAppUnlock()
                authDiskSource.storeUserBiometricUnlockKey(biometricsKey = encryptedBiometricsKey)
                authDiskSource.userBiometricKeyInitVector = cipher.iv
            }
            .fold(
                onSuccess = { BiometricsKeyResult.Success },
                onFailure = { BiometricsKeyResult.Error(error = it) },
            )

    override suspend fun unlockWithBiometrics(cipher: Cipher): BiometricsUnlockResult {
        val biometricsKey = authDiskSource
            .getUserBiometricUnlockKey()
            ?: return BiometricsUnlockResult.InvalidStateError(
                error = MissingPropertyException("Biometric key"),
            )
        val iv = authDiskSource.userBiometricKeyInitVector
        val decryptedUserKey = iv
            ?.let {
                try {
                    cipher
                        .doFinal(biometricsKey.toByteArray(Charsets.ISO_8859_1))
                        .decodeToString()
                } catch (e: GeneralSecurityException) {
                    Timber.w(e, "unlockWithBiometrics failed when decrypting biometrics key")
                    return BiometricsUnlockResult.BiometricDecodingError(error = e)
                }
            }
            ?: biometricsKey

        if (iv == null) {
            // Attempting to setup an encrypted pin before unlocking, if this fails we send back
            // the biometrics error and users will need to sign in another way and re-setup
            // biometrics.
            val encryptedBiometricsKey = try {
                cipher
                    .doFinal(decryptedUserKey.encodeToByteArray())
                    .toString(Charsets.ISO_8859_1)
            } catch (e: GeneralSecurityException) {
                Timber.w(e, "unlockWithBiometrics failed to migrate the user to IV encryption")
                return BiometricsUnlockResult.BiometricDecodingError(error = e)
            }
            // We now store the newly encrypted key and the associated IV for future use
            // since we want to migrate the user to a more secure form of biometrics.
            authDiskSource.storeUserBiometricUnlockKey(biometricsKey = encryptedBiometricsKey)
            authDiskSource.userBiometricKeyInitVector = cipher.iv
        }
        // Unlock the app here for state-based navigation to trigger.
        appLockManager.manualAppUnlock()
        return BiometricsUnlockResult.Success
    }

    override fun updateLastActiveTime() {
        authDiskSource.storeLastActiveTimeMillis(
            lastActiveTimeMillis = realtimeManager.elapsedRealtimeMs,
        )
    }
}
