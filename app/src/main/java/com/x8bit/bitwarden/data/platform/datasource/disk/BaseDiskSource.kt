package com.x8bit.bitwarden.data.platform.datasource.disk

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.core.content.edit

/**
 * Base class for simplifying interactions with [SharedPreferences].
 */
abstract class BaseDiskSource(
    private val sharedPreferences: SharedPreferences,
) : OnSharedPreferenceChangeListener {

    init {
        @Suppress("LeakingThis")
        sharedPreferences
            .registerOnSharedPreferenceChangeListener(this)
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
