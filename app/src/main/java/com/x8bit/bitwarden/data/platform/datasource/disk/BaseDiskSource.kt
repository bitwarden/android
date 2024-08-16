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
     * Gets the [Boolean] for the given [key] from [SharedPreferences], or returns `null` if that
     * key is not present.
     */
    protected fun getBoolean(key: String): Boolean? =
        if (sharedPreferences.contains(key.withBase())) {
            sharedPreferences.getBoolean(key.withBase(), false)
        } else {
            // Make sure we can return a null value as a default if necessary
            null
        }

    /**
     * Puts the [value] in [SharedPreferences] for the given [key] (or removes the key when the
     * value is `null`).
     */
    protected fun putBoolean(
        key: String,
        value: Boolean?,
    ): Unit =
        sharedPreferences.edit {
            if (value != null) {
                putBoolean(key.withBase(), value)
            } else {
                remove(key.withBase())
            }
        }

    /**
     * Gets the [Int] for the given [key] from [SharedPreferences], or returns `null` if that key
     * is not present.
     */
    protected fun getInt(key: String): Int? =
        if (sharedPreferences.contains(key.withBase())) {
            sharedPreferences.getInt(key.withBase(), 0)
        } else {
            // Make sure we can return a null value as a default if necessary
            null
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
                putInt(key.withBase(), value)
            } else {
                remove(key.withBase())
            }
        }

    /**
     * Gets the [Long] for the given [key] from [SharedPreferences], or returns `null` if that key
     * is not present.
     */
    protected fun getLong(key: String): Long? =
        if (sharedPreferences.contains(key.withBase())) {
            sharedPreferences.getLong(key.withBase(), 0)
        } else {
            // Make sure we can return a null value as a default if necessary
            null
        }

    /**
     * Puts the [value] in [SharedPreferences] for the given [key] (or removes the key when the
     * value is `null`).
     */
    protected fun putLong(
        key: String,
        value: Long?,
    ): Unit =
        sharedPreferences.edit {
            if (value != null) {
                putLong(key.withBase(), value)
            } else {
                remove(key.withBase())
            }
        }

    protected fun getString(
        key: String,
    ): String? = sharedPreferences.getString(key.withBase(), null)

    protected fun putString(
        key: String,
        value: String?,
    ): Unit = sharedPreferences.edit { putString(key.withBase(), value) }

    protected fun removeWithPrefix(prefix: String) {
        sharedPreferences
            .all
            .keys
            .filter { it.startsWith(prefix.withBase()) }
            .forEach { sharedPreferences.edit { remove(it) } }
    }

    protected fun String.appendIdentifier(identifier: String): String = "${this}_$identifier"
}

/**
 * Helper method for prepending the key with the appropriate base storage key.
 */
private fun String.withBase(): String = "bwPreferencesStorage:$this"
