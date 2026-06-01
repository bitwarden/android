package com.x8bit.bitwarden.data.platform.datasource.disk

import android.content.SharedPreferences
import androidx.core.content.edit
import com.bitwarden.core.data.util.decodeFromStringOrNull
import com.bitwarden.data.datasource.disk.BaseEncryptedDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.model.CookieConfigurationData
import kotlinx.serialization.json.Json

private const val CONFIG_PREFIX = "elb_cookie_config"
private const val ENCRYPTED_PREFIX = "bwSecureStorage:$CONFIG_PREFIX"

/**
 * Implementation of [CookieDiskSource] using encrypted SharedPreferences.
 *
 * Simple storage layer for cookies.
 */
class CookieDiskSourceImpl(
    sharedPreferences: SharedPreferences,
    private val encryptedSharedPreferences: SharedPreferences,
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

    override fun storeCookieConfig(hostname: String, config: CookieConfigurationData?) {
        val key = CONFIG_PREFIX.appendIdentifier(hostname)
        putEncryptedString(key, config?.let { json.encodeToString(it) })
    }

    override fun clearCookies() {
        val keysToRemove = encryptedSharedPreferences
            .all
            .keys
            .filter { it.startsWith(ENCRYPTED_PREFIX) }
        encryptedSharedPreferences.edit {
            keysToRemove.forEach { key -> remove(key) }
        }
    }
}
