package com.bitwarden.network.util

import com.bitwarden.network.model.JwtTokenDataJson
import kotlinx.serialization.json.Json
import timber.log.Timber

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
@Suppress("MagicNumber", "TooGenericExceptionCaught")
fun parseJwtTokenDataOrNull(jwtToken: String): JwtTokenDataJson? {
    val parts = jwtToken.split(".")
    if (parts.size != 3) {
        Timber.e(IllegalArgumentException("Incorrect number of parts"), "Invalid JWT Token")
        return null
    }

    val dataJson = parts[1]
    val decodedDataJson = dataJson.base64UrlDecodeOrNull() ?: run {
        Timber.e(IllegalArgumentException("Unable to decode"), "Invalid JWT Token")
        return null
    }

    return try {
        json.decodeFromString<JwtTokenDataJson>(decodedDataJson)
    } catch (throwable: Throwable) {
        Timber.e(throwable, "Failed to decode JwtTokenDataJson")
        null
    }
}
