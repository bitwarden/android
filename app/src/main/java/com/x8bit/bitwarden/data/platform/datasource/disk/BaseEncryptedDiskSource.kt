package com.x8bit.bitwarden.data.platform.datasource.disk

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences

/**
 * Base class for simplifying interactions with [SharedPreferences] and
 * [EncryptedSharedPreferences].
 */
@Suppress("UnnecessaryAbstractClass")
abstract class BaseEncryptedDiskSource(
    sharedPreferences: SharedPreferences,
    private val encryptedSharedPreferences: SharedPreferences,
) : BaseDiskSource(
    sharedPreferences = sharedPreferences,
) {
    protected fun getEncryptedString(
        key: String,
        default: String? = null,
    ): String? = encryptedSharedPreferences.getString(key.withBase(), default)

    protected fun putEncryptedString(
        key: String,
        value: String?,
    ): Unit = encryptedSharedPreferences.edit { putString(key.withBase(), value) }
}

/**
 * Helper method for prepending the key with the appropriate base storage key.
 */
private fun String.withBase(): String = "bwSecureStorage:$this"
