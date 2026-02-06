package com.x8bit.bitwarden.data.platform.datasource.disk

import android.content.SharedPreferences
import androidx.core.content.edit
import com.bitwarden.core.data.util.decodeFromStringOrNull
import com.x8bit.bitwarden.data.platform.datasource.disk.model.CookieConfigurationData
import kotlinx.serialization.json.Json

private const val CONFIG_PREFIX = "elb_cookie_config_"

/**
 * Implementation of [CookieDiskSource] using encrypted SharedPreferences.
 *
 * Simple storage layer for cookies.
 */
class CookieDiskSourceImpl(
    private val encryptedSharedPreferences: SharedPreferences,
    private val json: Json,
) : CookieDiskSource {

    override fun getCookieConfig(hostname: String): CookieConfigurationData? {
        val key = "$CONFIG_PREFIX$hostname"
        return encryptedSharedPreferences.getString(key, null)
            ?.let { json.decodeFromStringOrNull<CookieConfigurationData>(it) }
    }

    override fun storeCookieConfig(hostname: String, config: CookieConfigurationData) {
        val key = "$CONFIG_PREFIX$hostname"
        encryptedSharedPreferences.edit {
            putString(key, json.encodeToString(config))
        }
    }
}
