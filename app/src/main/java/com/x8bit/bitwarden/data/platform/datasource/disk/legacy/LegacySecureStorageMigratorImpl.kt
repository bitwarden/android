package com.x8bit.bitwarden.data.platform.datasource.disk.legacy

import android.content.SharedPreferences
import androidx.core.content.edit
import com.x8bit.bitwarden.data.platform.datasource.disk.BaseEncryptedDiskSource

/**
 * Primary implementation of [LegacySecureStorageMigrator].
 */
class LegacySecureStorageMigratorImpl(
    private val legacySecureStorage: LegacySecureStorage,
    private val encryptedSharedPreferences: SharedPreferences,
) : LegacySecureStorageMigrator {

    override fun migrateIfNecessary() {
        // If there are no remaining keys, there is no migration to perform.
        val keys = legacySecureStorage.getRawKeys()
        if (keys.isEmpty()) return

        // For now we are primarily concerned with keys that have not been hashed before storage,
        // which will all start with "bwSecureStorage". Hashing only occurred on devices with
        // SDK <23.
        val plaintextKeys = keys.filter {
            it.startsWith(BaseEncryptedDiskSource.ENCRYPTED_BASE_KEY)
        }

        plaintextKeys.forEach { unhashedKey ->
            val decryptedValue = legacySecureStorage.get(unhashedKey)
            encryptedSharedPreferences.edit {
                putString(unhashedKey, decryptedValue)
            }
            legacySecureStorage.remove(unhashedKey)
        }
    }
}
