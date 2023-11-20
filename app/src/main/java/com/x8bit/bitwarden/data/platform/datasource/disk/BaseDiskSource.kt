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
