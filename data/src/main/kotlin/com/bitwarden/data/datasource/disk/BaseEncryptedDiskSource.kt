package com.bitwarden.data.datasource.disk

import android.content.SharedPreferences
import androidx.core.content.edit

private const val LEGACY_PREFIX: String = "bwSecureStorage:"

/**
 * Base class for simplifying interactions with [SharedPreferences], this includes both regular
 * and encrypted storage.
 */
@Suppress("UnnecessaryAbstractClass")
abstract class BaseEncryptedDiskSource(
    sharedPreferences: SharedPreferences,
    private val encryptedSharedPreferences: SharedPreferences,
    private val keystoreEncryptedPreferences: SharedPreferences,
) : BaseDiskSource(sharedPreferences = sharedPreferences) {
    protected fun getEncryptedString(
        key: String,
    ): String? = keystoreEncryptedPreferences.getString(key, null)

    protected fun putEncryptedString(
        key: String,
        value: String?,
    ) {
        keystoreEncryptedPreferences.edit { putString(key, value) }
    }

    protected fun migrateKeyByPrefix(keyPrefix: String) {
        encryptedSharedPreferences
            .all
            .keys
            .filter { it.startsWith(prefix = "$LEGACY_PREFIX$keyPrefix") }
            .forEach { key ->
                // Move the value to the new file without the base prefix.
                encryptedSharedPreferences.getString(key, null)?.let { value ->
                    keystoreEncryptedPreferences.edit(commit = true) {
                        putString(key.removePrefix(prefix = LEGACY_PREFIX), value)
                    }
                }
                // Then ensure the original value is deleted.
                encryptedSharedPreferences.edit(commit = true) { remove(key) }
            }
    }
}
