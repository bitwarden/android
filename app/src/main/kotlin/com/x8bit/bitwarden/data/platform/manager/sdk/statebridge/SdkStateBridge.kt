package com.x8bit.bitwarden.data.platform.manager.sdk.statebridge

import com.bitwarden.core.MasterPasswordUnlockData
import com.bitwarden.core.StateBridgeForeignImpl
import com.bitwarden.core.V2UpgradeToken
import com.bitwarden.core.WrappedAccountCryptographicState
import com.bitwarden.crypto.EncString
import com.bitwarden.crypto.Kdf
import com.bitwarden.crypto.PasswordProtectedKeyEnvelope
import com.bitwarden.crypto.SymmetricCryptoKey
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.sdk.util.toKdfRequestModel
import com.x8bit.bitwarden.data.auth.repository.util.toSdkParams
import com.x8bit.bitwarden.data.auth.repository.util.updateKdf
import com.x8bit.bitwarden.data.auth.repository.util.updateMasterPasswordUnlock
import com.x8bit.bitwarden.data.vault.repository.util.toSdkMasterPasswordUnlock
import com.x8bit.bitwarden.data.vault.repository.util.toV2UpgradeToken
import com.x8bit.bitwarden.data.vault.repository.util.toV2UpgradeTokenJson

/**
 * A user-scoped implementation of a Bitwarden SDK [StateBridgeForeignImpl].
 */
@Suppress("TooManyFunctions")
internal class SdkStateBridge(
    private val userId: String,
    private val authDiskSource: AuthDiskSource,
) : StateBridgeForeignImpl {
    @Volatile
    private var inMemoryUserKey: SymmetricCryptoKey? = null

    override suspend fun setUserKey(value: SymmetricCryptoKey) {
        inMemoryUserKey = value
    }

    override suspend fun getUserKey(): SymmetricCryptoKey? = inMemoryUserKey

    override suspend fun clearUserKey() {
        inMemoryUserKey = null
    }

    override suspend fun setPersistentPinEnvelope(value: PasswordProtectedKeyEnvelope) {
        authDiskSource.storePersistentPinProtectedUserKeyEnvelope(
            userId = userId,
            pinProtectedUserKeyEnvelope = value,
        )
    }

    override suspend fun getPersistentPinEnvelope(): PasswordProtectedKeyEnvelope? =
        authDiskSource.getPersistentPinProtectedUserKeyEnvelope(userId = userId)

    override suspend fun clearPersistentPinEnvelope() {
        authDiskSource.storePersistentPinProtectedUserKeyEnvelope(
            userId = userId,
            pinProtectedUserKeyEnvelope = null,
        )
    }

    override suspend fun setEphemeralPinEnvelope(value: PasswordProtectedKeyEnvelope) {
        authDiskSource.storeEphemeralPinProtectedUserKeyEnvelope(
            userId = userId,
            pinProtectedUserKeyEnvelope = value,
        )
    }

    override suspend fun getEphemeralPinEnvelope(): PasswordProtectedKeyEnvelope? =
        authDiskSource.getEphemeralPinProtectedUserKeyEnvelope(userId = userId)

    override suspend fun clearEphemeralPinEnvelope() {
        authDiskSource.storeEphemeralPinProtectedUserKeyEnvelope(
            userId = userId,
            pinProtectedUserKeyEnvelope = null,
        )
    }

    override suspend fun setEncryptedPin(value: EncString) {
        authDiskSource.storeEncryptedPin(userId = userId, encryptedPin = value)
    }

    override suspend fun getEncryptedPin(): EncString? =
        authDiskSource.getEncryptedPin(userId = userId)

    override suspend fun clearEncryptedPin() {
        authDiskSource.storeEncryptedPin(userId = userId, encryptedPin = null)
    }

    override suspend fun setV2UpgradeToken(value: V2UpgradeToken) {
        authDiskSource.storeV2UpgradeToken(
            userId = userId,
            v2UpgradeToken = value.toV2UpgradeTokenJson(),
        )
    }

    override suspend fun getV2UpgradeToken(): V2UpgradeToken? =
        authDiskSource
            .getV2UpgradeToken(userId = userId)
            ?.toV2UpgradeToken()

    override suspend fun clearV2UpgradeToken() {
        authDiskSource.storeV2UpgradeToken(userId = userId, v2UpgradeToken = null)
    }

    override suspend fun setAccountCryptographicState(value: WrappedAccountCryptographicState) {
        authDiskSource.storeAccountCryptographicState(
            userId = userId,
            accountCryptographicState = value,
        )
    }

    override suspend fun getAccountCryptographicState(): WrappedAccountCryptographicState? =
        authDiskSource.getAccountCryptographicState(userId = userId)

    override suspend fun clearAccountCryptographicState() {
        authDiskSource.storeAccountCryptographicState(
            userId = userId,
            accountCryptographicState = null,
        )
    }

    override suspend fun setMasterpasswordUnlockData(value: MasterPasswordUnlockData) {
        authDiskSource.userState = authDiskSource.userState?.updateMasterPasswordUnlock(
            userId = userId,
            masterPasswordUnlock = value,
        )
    }

    override suspend fun getMasterpasswordUnlockData(): MasterPasswordUnlockData? =
        authDiskSource
            .userState
            ?.accounts[userId]
            ?.profile
            ?.userDecryptionOptions
            ?.masterPasswordUnlock
            ?.toSdkMasterPasswordUnlock()

    override suspend fun clearMasterpasswordUnlockData() {
        authDiskSource.userState = authDiskSource.userState?.updateMasterPasswordUnlock(
            userId = userId,
            masterPasswordUnlock = null,
        )
    }

    override suspend fun getKdfConfig(): Kdf? =
        authDiskSource.userState?.accounts[userId]?.profile?.toSdkParams()

    override suspend fun setKdfConfig(value: Kdf) {
        authDiskSource.userState = authDiskSource.userState?.updateKdf(
            userId = userId,
            kdf = value.toKdfRequestModel(),
        )
    }

    override suspend fun clearKdfConfig() {
        authDiskSource.userState = authDiskSource.userState?.updateKdf(
            userId = userId,
            kdf = null,
        )
    }
}
