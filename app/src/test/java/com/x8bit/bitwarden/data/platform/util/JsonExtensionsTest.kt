package com.x8bit.bitwarden.data.platform.util

import com.x8bit.bitwarden.data.util.assertJsonEquals
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class JsonExtensionsTest {
    private val json = Json

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

    @Test
    fun `transformPascalKeysToCamelCase should transform keys with '-' or '_' to camelCase`() {
        val jsonData = json.parseToJsonElement("""[{"kebab-array":[{"snake_case":0}]}]""")
        assertJsonEquals(
            """[{"kebabArray":[{"snakeCase":0}]}]""",
            jsonData.transformKeysToCamelCase().toString(),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `transformKeysToCamelCase should return transformed response when root object is JSONArray`() {
        val jsonData = Json.parseToJsonElement("""[{"PascalArray":[{"PascalCase":0}]}]""")
        assertJsonEquals(
            """[{"pascalArray":[{"pascalCase":0}]}]""",
            jsonData.transformKeysToCamelCase().toString(),
        )
    }

    @Test
    fun `parseToJsonElementOrNull should return null when json is empty string`() {
        assertNull(json.parseToJsonElementOrNull(""))
    }

    @Test
    fun `parseToJsonElementOrNull should return null when json is invalid`() {
        assertNull(json.parseToJsonElementOrNull("{OK}"))
    }
}

@Serializable
private data class TestData(
    @SerialName("data") val data: String,
)
