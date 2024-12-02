package com.x8bit.bitwarden.data.platform.util

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
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

/**
 * Attempts to decode the given JSON [string] into the given type [T]. If there is an error in
 * processing the JSON or deserializing, the exception is still throw after [onFailure] lambda is
 * invoked.
 */
inline fun <reified T> Json.decodeFromStringWithErrorCallback(
    string: String,
    onFailure: (throwable: Throwable) -> Unit,
): T =
    try {
        decodeFromString(string = string)
    } catch (se: SerializationException) {
        onFailure(se)
        throw se
    } catch (iae: IllegalArgumentException) {
        onFailure(iae)
        throw iae
    }
