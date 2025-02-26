package com.bitwarden.authenticator.data.platform.util

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Attempts to decode the given JSON [string] into the given type [T]. If there is an error in
 * processing the JSON or deserializing it to an instance of [T], `null` will be returned.
 */
inline fun <reified T> Json.decodeFromStringOrNull(
    string: String,
): T? =
    try {
        decodeFromString(string = string)
    } catch (e: SerializationException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }
