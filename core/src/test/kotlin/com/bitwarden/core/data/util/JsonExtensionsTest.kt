package com.bitwarden.core.data.util

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class JsonExtensionsTest {
    private val json = Json

    @Test
    fun `decodeFromJsonElementOrNull for valid JSON but an incorrect model should return null`() {
        assertNull(
            json.decodeFromJsonElementOrNull(
                deserializer = TestData.serializer(),
                element = buildJsonObject {},
            ),
        )
    }

    @Test
    fun `decodeFromJsonElementOrNull for valid JSON and a correct model should parse correctly`() {
        assertEquals(
            TestData(data = "test"),
            json.decodeFromJsonElementOrNull(
                deserializer = TestData.serializer(),
                element = buildJsonObject {
                    put(key = "data", value = "test")
                },
            ),
        )
    }

    @Test
    fun `decodeFromStringOrNull for invalid JSON should return null`() {
        assertNull(
            json.decodeFromStringOrNull<TestData>(
                """
                {]
                """,
            ),
        )
    }

    @Test
    fun `decodeFromStringOrNull for valid JSON but an incorrect model should return null`() {
        assertNull(
            json.decodeFromStringOrNull<TestData>(
                """
                {}
                """,
            ),
        )
    }

    @Test
    fun `decodeFromStringOrNull for valid JSON and a correct model should parse correctly`() {
        assertEquals(
            TestData(
                data = "test",
            ),
            json.decodeFromStringOrNull<TestData>(
                """
                {
                  "data": "test"
                }
                """,
            ),
        )
    }
}

@Serializable
private data class TestData(
    @SerialName("data") val data: String,
)
