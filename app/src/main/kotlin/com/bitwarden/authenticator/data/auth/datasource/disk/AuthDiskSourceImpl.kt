package com.bitwarden.authenticator.data.auth.datasource.disk

import android.content.SharedPreferences
import com.bitwarden.authenticator.data.platform.datasource.disk.BaseDiskSource.Companion.BASE_KEY
import com.bitwarden.authenticator.data.platform.datasource.disk.BaseEncryptedDiskSource

private const val LAST_ACTIVE_TIME_KEY = "$BASE_KEY:lastActiveTime"

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
}
