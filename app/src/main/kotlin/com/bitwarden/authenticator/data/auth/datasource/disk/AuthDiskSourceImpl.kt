package com.bitwarden.authenticator.data.auth.datasource.disk

import android.content.SharedPreferences
import com.bitwarden.authenticator.data.platform.datasource.disk.BaseDiskSource.Companion.BASE_KEY
import com.bitwarden.authenticator.data.platform.datasource.disk.BaseEncryptedDiskSource
import com.bitwarden.authenticator.data.platform.datasource.disk.BaseEncryptedDiskSource.Companion.ENCRYPTED_BASE_KEY

private const val LAST_ACTIVE_TIME_KEY = "$BASE_KEY:lastActiveTime"
private const val BIOMETRICS_UNLOCK_KEY = "$ENCRYPTED_BASE_KEY:userKeyBiometricUnlock"

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
}
