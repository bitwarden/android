package com.x8bit.bitwarden.authenticator.data.platform.datasource.disk

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
    ): String? = encryptedSharedPreferences.getString(key, default)

    protected fun putEncryptedString(
        key: String,
        value: String?,
    ): Unit = encryptedSharedPreferences.edit { putString(key, value) }
}
