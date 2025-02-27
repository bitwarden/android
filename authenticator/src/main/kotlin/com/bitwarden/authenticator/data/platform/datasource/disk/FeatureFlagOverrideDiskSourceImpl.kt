package com.bitwarden.authenticator.data.platform.datasource.disk

import android.content.SharedPreferences
import com.bitwarden.authenticator.data.platform.manager.model.FlagKey

/**
 * Default implementation of the [FeatureFlagOverrideDiskSource]
 */
class FeatureFlagOverrideDiskSourceImpl(
    sharedPreferences: SharedPreferences,
) : FeatureFlagOverrideDiskSource, BaseDiskSource(sharedPreferences) {

    override fun <T : Any> saveFeatureFlag(key: FlagKey<T>, value: T) {
        when (key.defaultValue) {
            is Boolean -> putBoolean(key.keyName, value as Boolean)
            is String -> putString(key.keyName, value as String)
            is Int -> putInt(key.keyName, value as Int)
            else -> Unit
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getFeatureFlag(key: FlagKey<T>): T? {
        return try {
            when (key.defaultValue) {
                is Boolean -> getBoolean(key.keyName) as? T
                is String -> getString(key.keyName) as? T
                is Int -> getInt(key.keyName) as? T
                else -> null
            }
        } catch (castException: ClassCastException) {
            null
        } catch (numberFormatException: NumberFormatException) {
            null
        }
    }
}
