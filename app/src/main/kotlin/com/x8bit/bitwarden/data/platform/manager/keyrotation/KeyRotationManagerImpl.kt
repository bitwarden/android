package com.x8bit.bitwarden.data.platform.manager.keyrotation

import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.core.data.util.flatMap
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

/**
 * THe default implementation of the [KeyRotationManager].
 */
internal class KeyRotationManagerImpl(
    private val authDiskSource: AuthDiskSource,
    private val vaultSdkSource: VaultSdkSource,
) : KeyRotationManager {
    private val mutableShouldRotateBiometricKey = bufferedMutableSharedFlow<Unit>()
    override val shouldRotateBiometricKey: Flow<Unit> = mutableShouldRotateBiometricKey

    override suspend fun rotateAuthenticatorSyncKey(userId: String) {
        authDiskSource
            .getAuthenticatorSyncUnlockKey(userId = userId)
            ?.let { getRotatedKeyIfNecessary(userId = userId, keyToRotate = it) }
            ?.let {
                Timber.i("Rotating Authenticator Sync Unlock Key")
                // Store the new key to disk for future usage.
                authDiskSource.storeAuthenticatorSyncUnlockKey(
                    userId = userId,
                    authenticatorSyncUnlockKey = it,
                )
            }
    }

    override suspend fun rotateAutoUnlockKey(userId: String) {
        authDiskSource
            .getUserAutoUnlockKey(userId = userId)
            ?.let { getRotatedKeyIfNecessary(userId = userId, keyToRotate = it) }
            ?.let {
                Timber.i("Rotating Auto Unlock Key")
                // Store the new key to disk for future usage.
                authDiskSource.storeUserAutoUnlockKey(userId = userId, userAutoUnlockKey = it)
            }
    }

    override suspend fun rotateBiometricsKey(userId: String, decryptedBiometricsUserKey: String) {
        this
            .getRotatedKeyIfNecessary(userId = userId, keyToRotate = decryptedBiometricsUserKey)
            ?.let { _ ->
                Timber.i("Prompting to rotate Biometrics Key")
                // We need user interaction to rotate this key, so we emit here to prompt the user.
                mutableShouldRotateBiometricKey.emit(Unit)
            }
    }

    /**
     * Returns the new key that should be used if the provided [keyToRotate] needs to be rotated.
     * `null` is returned if the key should not or cannot be rotated.
     */
    private suspend fun getRotatedKeyIfNecessary(
        userId: String,
        keyToRotate: String,
    ): String? =
        vaultSdkSource
            .getKeyId(userKey = keyToRotate)
            .flatMap { keyId ->
                vaultSdkSource
                    .getUserEncryptionKey(userId = userId)
                    .flatMap { encryptedUserKey ->
                        vaultSdkSource
                            .getKeyId(userKey = encryptedUserKey)
                            .map { encryptedUserKey.takeIf { _ -> keyId != it } }
                    }
            }
            .getOrNull()
}
