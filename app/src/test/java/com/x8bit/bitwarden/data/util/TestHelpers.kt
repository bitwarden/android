package com.x8bit.bitwarden.data.util

import com.x8bit.bitwarden.data.platform.datasource.network.di.PlatformNetworkModule
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * Helper method for comparing JSON string and ignoring the formatting.
 */
fun assertJsonEquals(
    expected: String,
    actual: String,
    json: Json = PlatformNetworkModule.providesJson(),
) {
    assertEquals(
        json.parseToJsonElement(expected),
        json.parseToJsonElement(actual),
    )
}
