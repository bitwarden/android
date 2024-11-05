package com.x8bit.bitwarden.data.platform.util

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.map

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

/**
 * Safely parses a JSON string to a JsonElement, returning null if the parsing fails.
 */
fun Json.parseToJsonElementOrNull(json: String): JsonElement? = try {
    parseToJsonElement(json)
} catch (_: SerializationException) {
    null
}

/**
 * Recursively transforms keys of a JsonElement to camel case.
 *
 * This function handles both JsonObject and JsonArray, converting keys within JsonObjects to camel
 * case.
 *
 * @return A new JsonElement with transformed keys.
 */
fun JsonElement.transformKeysToCamelCase(): JsonElement =
    when (this) {
        is JsonObject -> buildJsonObject {
            this@transformKeysToCamelCase.entries
                .forEach { (key: String, value: JsonElement) ->
                    val transformedKey = if (key.contains("-") || key.contains("_")) {
                        key
                            .lowercase()
                            .split("_", "-")
                            .mapIndexed { index, originalKey ->
                                if (index > 0) {
                                    originalKey.replaceFirstChar { it.uppercase() }
                                } else {
                                    originalKey.lowercase()
                                }
                            }
                            .joinToString("")
                    } else {
                        key.replaceFirstChar { it.lowercase() }
                    }
                    this@buildJsonObject.put(transformedKey, value.transformKeysToCamelCase())
                }
        }

        is JsonArray -> JsonArray(this.map { it.transformKeysToCamelCase() })
        else -> this
    }
