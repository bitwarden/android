package com.x8bit.bitwarden.data.platform.datasource.disk

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Base class for simplifying interactions with [SharedPreferences].
 */
@Suppress("UnnecessaryAbstractClass")
abstract class BaseDiskSource(
    private val sharedPreferences: SharedPreferences,
) {
    /**
     * Gets the [Int] for the given [key] from [SharedPreferences], or return the [default] value
     * if that key is not present.
     */
    protected fun getInt(
        key: String,
        default: Int? = null,
    ): Int? =
        if (sharedPreferences.contains(key)) {
            sharedPreferences.getInt(key, 0)
        } else {
            // Make sure we can return a null value as a default if necessary
            default
        }

    /**
     * Puts the [value] in [SharedPreferences] for the given [key] (or removes the key when the
     * value is `null`).
     */
    protected fun putInt(
        key: String,
        value: Int?,
    ): Unit =
        sharedPreferences.edit {
            if (value != null) {
                putInt(key, value)
            } else {
                remove(key)
            }
        }

    protected fun getString(
        key: String,
        default: String? = null,
    ): String? = sharedPreferences.getString(key, default)

    protected fun putString(
        key: String,
        value: String?,
    ): Unit = sharedPreferences.edit { putString(key, value) }

    companion object {
        const val BASE_KEY: String = "bwPreferencesStorage"
    }
}
