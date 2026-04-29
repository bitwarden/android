package com.bitwarden.core.data.util

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Attempts to decode the given JSON [element] into the given type [T]. If there is an error in
 * processing the JSON or deserializing it to an instance of [T], `null` will be returned.
 */
fun <T> Json.decodeFromJsonElementOrNull(
    deserializer: DeserializationStrategy<T>,
    element: JsonElement,
): T? =
    try {
        decodeFromJsonElement(deserializer = deserializer, element = element)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

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
