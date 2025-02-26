package com.bitwarden.authenticator.data.platform.base

import android.content.SharedPreferences

/**
 * A faked implementation of [SharedPreferences] that is backed by an internal, memory-based map.
 */
class FakeSharedPreferences : SharedPreferences {
    private val sharedPreferences: MutableMap<String, Any?> = mutableMapOf()
    private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun contains(key: String): Boolean =
        sharedPreferences.containsKey(key)

    override fun edit(): SharedPreferences.Editor = Editor()

    override fun getAll(): Map<String, *> = sharedPreferences

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        getValue(key, defaultValue)

    override fun getFloat(key: String, defaultValue: Float): Float =
        getValue(key, defaultValue)

    override fun getInt(key: String, defaultValue: Int): Int =
        getValue(key, defaultValue)

    override fun getLong(key: String, defaultValue: Long): Long =
        getValue(key, defaultValue)

    override fun getString(key: String, defaultValue: String?): String? =
        getValue(key, defaultValue)

    override fun getStringSet(key: String, defaultValue: Set<String>?): Set<String>? =
        getValue(key, defaultValue)

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener,
    ) {
        listeners += listener
    }

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener,
    ) {
        listeners -= listener
    }

    private inline fun <reified T> getValue(
        key: String,
        defaultValue: T,
    ): T = sharedPreferences[key] as? T ?: defaultValue

    inner class Editor : SharedPreferences.Editor {
        private val pendingSharedPreferences = sharedPreferences.toMutableMap()

        override fun apply() {
            sharedPreferences.apply {
                clear()
                putAll(pendingSharedPreferences)

                // Notify listeners
                listeners.forEach { listener ->
                    pendingSharedPreferences.keys.forEach { key ->
                        listener.onSharedPreferenceChanged(this@FakeSharedPreferences, key)
                    }
                }
            }
        }

        override fun clear(): SharedPreferences.Editor =
            apply { pendingSharedPreferences.clear() }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor =
            putValue(key, value)

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor =
            putValue(key, value)

        override fun putInt(key: String, value: Int): SharedPreferences.Editor =
            putValue(key, value)

        override fun putLong(key: String, value: Long): SharedPreferences.Editor =
            putValue(key, value)

        override fun putString(key: String, value: String?): SharedPreferences.Editor =
            putValue(key, value)

        override fun putStringSet(key: String, value: Set<String>?): SharedPreferences.Editor =
            putValue(key, value)

        override fun remove(key: String): SharedPreferences.Editor =
            apply { pendingSharedPreferences.remove(key) }

        private inline fun <reified T> putValue(
            key: String,
            value: T,
        ): SharedPreferences.Editor = apply {
            value
                ?.let { pendingSharedPreferences[key] = it }
                ?: pendingSharedPreferences.remove(key)
        }
    }
}
