package com.x8bit.bitwarden.data.platform.datasource.disk.legacy

import androidx.security.crypto.EncryptedSharedPreferences

/**
 * Represents a legacy storage system that exists only to migrate data to
 * [EncryptedSharedPreferences]. Because no new data will be stored here, only the ability to
 * retrieve and clear data is provided.
 */
interface LegacySecureStorage {
    /**
     * Returns the data for the given [key], or `null` if no data for that key is present or if
     * decryption has failed.
     */
    fun get(key: String): String?

    /**
     * Returns all of the raw keys stored. In some cases these will be hashed versions of the keys
     * passed to [get].
     */
    fun getRawKeys(): Set<String>

    /**
     * Removes the data for the given [key].
     */
    fun remove(key: String)

    /**
     * Removes all data stored.
     */
    fun removeAll()
}
