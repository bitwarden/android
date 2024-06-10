package com.x8bit.bitwarden.data.auth.repository.util

import com.x8bit.bitwarden.data.auth.repository.model.JwtTokenDataJson
import com.x8bit.bitwarden.data.platform.datasource.network.util.base64UrlDecodeOrNull
import kotlinx.serialization.json.Json

/**
 * Internal, generally basic [Json] instance for JWT parsing purposes.
 */
private val json: Json by lazy {
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
}

/**
 * Parses a [JwtTokenDataJson] from the given [jwtToken], or `null` if this parsing is not possible.
 */
@Suppress("MagicNumber", "ReturnCount")
fun parseJwtTokenDataOrNull(jwtToken: String): JwtTokenDataJson? {
    val parts = jwtToken.split(".")
    if (parts.size != 3) return null

    val dataJson = parts[1]
    val decodedDataJson = dataJson.base64UrlDecodeOrNull() ?: return null

    return try {
        json.decodeFromString<JwtTokenDataJson>(decodedDataJson)
    } catch (_: Throwable) {
        null
    }
}
