package com.x8bit.bitwarden.data.vault.manager

import com.bitwarden.core.EnrollPinResponse
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import timber.log.Timber

/**
 * The default implementation of the [PinProtectedUserKeyManager].
 */
internal class PinProtectedUserKeyManagerImpl(
    private val authDiskSource: AuthDiskSource,
    private val vaultSdkSource: VaultSdkSource,
) : PinProtectedUserKeyManager {
    override suspend fun deriveTemporaryPinProtectedUserKeyIfNecessary(userId: String) {
        val encryptedPin = authDiskSource.getEncryptedPin(userId = userId) ?: return
        if (authDiskSource.getPinProtectedUserKeyEnvelope(userId = userId) != null) return

        this
            .enrollPinWithEncryptedPin(
                userId = userId,
                encryptedPin = encryptedPin,
                inMemoryOnly = true,
            )
            .onSuccess {
                Timber.d("[Auth] Set PIN-protected user key in memory")
            }
    }

    override suspend fun migratePinProtectedUserKeyIfNeeded(userId: String) {
        val encryptedPin = authDiskSource.getEncryptedPin(userId = userId) ?: return
        if (authDiskSource.getPinProtectedUserKeyEnvelope(userId = userId) != null) return

        val inMemoryOnly = authDiskSource.getPinProtectedUserKey(userId = userId) == null
        this
            .enrollPinWithEncryptedPin(
                userId = userId,
                encryptedPin = encryptedPin,
                inMemoryOnly = inMemoryOnly,
            )
            .onSuccess {
                if (inMemoryOnly) {
                    Timber.d("[Auth] Set PIN-protected user key in memory")
                } else {
                    Timber.d("[Auth] Migrated from legacy PIN to PIN-protected user key envelope")
                }
            }
    }

    private suspend fun enrollPinWithEncryptedPin(
        userId: String,
        encryptedPin: String,
        inMemoryOnly: Boolean,
    ): Result<EnrollPinResponse> =
        vaultSdkSource
            .enrollPinWithEncryptedPin(userId = userId, encryptedPin = encryptedPin)
            .onSuccess { enrollPinResponse ->
                storePinData(
                    userId = userId,
                    encryptedPin = enrollPinResponse.userKeyEncryptedPin,
                    pinProtectedUserKeyEnvelope = enrollPinResponse.pinProtectedUserKeyEnvelope,
                    inMemoryOnly = inMemoryOnly,
                )
            }
            .onFailure {
                storePinData(
                    userId = userId,
                    encryptedPin = null,
                    pinProtectedUserKeyEnvelope = null,
                    inMemoryOnly = false,
                )
            }

    private fun storePinData(
        userId: String,
        encryptedPin: String?,
        pinProtectedUserKeyEnvelope: String?,
        inMemoryOnly: Boolean,
    ) {
        authDiskSource.storeEncryptedPin(userId = userId, encryptedPin = encryptedPin)
        authDiskSource.storePinProtectedUserKeyEnvelope(
            userId = userId,
            pinProtectedUserKeyEnvelope = pinProtectedUserKeyEnvelope,
            inMemoryOnly = inMemoryOnly,
        )
        // This property is deprecated and we should be migrated to the PinProtectedUserKeyEnvelope.
        // Because of this, we always clear this value and it should always be cleared at the disk
        // level, not the in-memory level.
        authDiskSource.storePinProtectedUserKey(
            userId = userId,
            pinProtectedUserKey = null,
            inMemoryOnly = false,
        )
    }
}
