package com.x8bit.bitwarden.data.platform.datasource.disk

import android.content.SharedPreferences
import com.bitwarden.core.data.util.decodeFromStringOrNull
import com.bitwarden.data.datasource.disk.BaseEncryptedDiskSource
import kotlinx.serialization.json.Json

private const val CUSTOM_HEADERS_PREFIX = "customHeaders"

/**
 * Implementation of [CustomHeadersDiskSource] using encrypted SharedPreferences.
 *
 * The header values may contain credentials, such as Cloudflare Access service tokens, so they
 * are only ever written to encrypted storage. The environment data referencing them stores only
 * the opaque identifier.
 */
class CustomHeadersDiskSourceImpl(
    sharedPreferences: SharedPreferences,
    encryptedSharedPreferences: SharedPreferences,
    private val json: Json,
) : CustomHeadersDiskSource,
    BaseEncryptedDiskSource(
        sharedPreferences = sharedPreferences,
        encryptedSharedPreferences = encryptedSharedPreferences,
    ) {

    override fun getCustomHeaders(id: String): Map<String, String>? =
        getEncryptedString(key = CUSTOM_HEADERS_PREFIX.appendIdentifier(id))
            ?.let { json.decodeFromStringOrNull<Map<String, String>>(it) }

    override fun storeCustomHeaders(id: String, headers: Map<String, String>?) {
        putEncryptedString(
            key = CUSTOM_HEADERS_PREFIX.appendIdentifier(id),
            value = headers?.let { json.encodeToString(it) },
        )
    }
}
