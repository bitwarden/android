package com.x8bit.bitwarden.data.platform.datasource.disk

import android.content.SharedPreferences
import com.bitwarden.core.data.util.decodeFromStringOrNull
import com.bitwarden.data.datasource.disk.BaseEncryptedDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.model.CookieConfigurationData
import kotlinx.serialization.json.Json

private const val CONFIG_PREFIX = "elb_cookie_config"

/**
 * Implementation of [CookieDiskSource] using encrypted SharedPreferences.
 *
 * Simple storage layer for cookies.
 */
class CookieDiskSourceImpl(
    sharedPreferences: SharedPreferences,
    encryptedSharedPreferences: SharedPreferences,
    private val json: Json,
) : CookieDiskSource,
    BaseEncryptedDiskSource(
        sharedPreferences = sharedPreferences,
        encryptedSharedPreferences = encryptedSharedPreferences,
    ) {

    override fun getCookieConfig(hostname: String): CookieConfigurationData? {
        val key = CONFIG_PREFIX.appendIdentifier(hostname)
        return getEncryptedString(key)
            ?.let { json.decodeFromStringOrNull<CookieConfigurationData>(it) }
    }

    override fun storeCookieConfig(hostname: String, config: CookieConfigurationData) {
        val key = CONFIG_PREFIX.appendIdentifier(hostname)
        putEncryptedString(key, json.encodeToString(config))
    }
}
