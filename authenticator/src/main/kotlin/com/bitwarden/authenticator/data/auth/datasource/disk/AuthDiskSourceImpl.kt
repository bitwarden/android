package com.bitwarden.authenticator.data.auth.datasource.disk

import android.content.SharedPreferences
import com.bitwarden.data.datasource.disk.BaseEncryptedDiskSource
import java.util.UUID

private const val AUTHENTICATOR_SYNC_SYMMETRIC_KEY = "authenticatorSyncSymmetricKey"
private const val LAST_ACTIVE_TIME_KEY = "lastActiveTime"
private const val BIOMETRICS_UNLOCK_KEY = "userKeyBiometricUnlock"
private const val UNIQUE_APP_ID_KEY = "appId"

/**
 * Primary implementation of [AuthDiskSource].
 */
class AuthDiskSourceImpl(
    encryptedSharedPreferences: SharedPreferences,
    sharedPreferences: SharedPreferences,
) : BaseEncryptedDiskSource(
    encryptedSharedPreferences = encryptedSharedPreferences,
    sharedPreferences = sharedPreferences,
),
    AuthDiskSource {

    override val uniqueAppId: String
        get() = getString(key = UNIQUE_APP_ID_KEY) ?: generateAndStoreUniqueAppId()

    override fun getLastActiveTimeMillis(): Long? =
        getLong(key = LAST_ACTIVE_TIME_KEY)

    override fun storeLastActiveTimeMillis(
        lastActiveTimeMillis: Long?,
    ) {
        putLong(
            key = LAST_ACTIVE_TIME_KEY,
            value = lastActiveTimeMillis,
        )
    }

    override fun getUserBiometricUnlockKey(): String? =
        getEncryptedString(key = BIOMETRICS_UNLOCK_KEY)

    override fun storeUserBiometricUnlockKey(
        biometricsKey: String?,
    ) {
        putEncryptedString(
            key = BIOMETRICS_UNLOCK_KEY,
            value = biometricsKey,
        )
    }

    override var authenticatorBridgeSymmetricSyncKey: ByteArray?
        set(value) {
            val asString = value?.let { value.toString(Charsets.ISO_8859_1) }
            putEncryptedString(AUTHENTICATOR_SYNC_SYMMETRIC_KEY, asString)
        }
        get() = getEncryptedString(AUTHENTICATOR_SYNC_SYMMETRIC_KEY)
            ?.toByteArray(Charsets.ISO_8859_1)

    private fun generateAndStoreUniqueAppId(): String =
        UUID
            .randomUUID()
            .toString()
            .also {
                putString(key = UNIQUE_APP_ID_KEY, value = it)
            }
}
