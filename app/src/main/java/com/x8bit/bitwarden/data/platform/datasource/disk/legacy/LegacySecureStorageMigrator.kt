package com.x8bit.bitwarden.data.platform.datasource.disk.legacy

import androidx.security.crypto.EncryptedSharedPreferences

/**
 * Provides the ability to migrate from a legacy "secure storage" system to
 * [EncryptedSharedPreferences].
 */
interface LegacySecureStorageMigrator {

    /**
     * Migrates any data from the legacy "secure storage" system to [EncryptedSharedPreferences].
     * After migration, data will be removed from the legacy system.
     */
    fun migrateIfNecessary()
}
