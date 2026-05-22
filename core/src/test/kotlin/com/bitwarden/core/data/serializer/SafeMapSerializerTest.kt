package com.bitwarden.core.data.serializer

import com.bitwarden.core.di.CoreModule
import io.mockk.mockk
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SafeMapSerializerTest {
    private val json = CoreModule.providesJson(buildInfoManager = mockk(relaxed = true))

    @Test
    fun `deserializes JSON successfully with invalid items`() {
        assertEquals(
            SampleClass(data = mapOf("foo" to SampleClass.InnerSampleClass(string = "test"))),
            json.decodeFromString<SampleClass>(
                """
                {
                  "data": {
                    "bar": {
                      "string": 5
                    },
                    "foo": {
                      "string": "test"
                    }
                  }
                }
                """,
            ),
        )
    }

    @Test
    fun `deserializes JSON successfully with null list`() {
        assertEquals(
            SampleClass(data = null),
            json.decodeFromString<SampleClass>(
                """
                {
                  "data": null
                }
                """,
            ),
        )
    }

    @Test
    fun `serialized JSON successfully`() {
        assertEquals(
            buildJsonObject {
                putJsonObject(key = "data") {
                    putJsonObject("foo") {
                        put(key = "string", value = "test")
                    }
                }
            },
            json.encodeToJsonElement(
                serializer = SampleClass.serializer(),
                value = SampleClass(
                    data = mapOf("foo" to SampleClass.InnerSampleClass(string = "test")),
                ),
            ),
        )
    }
}

@Serializable
private data class SampleClass(
    @Serializable(with = SafeMapSerializer::class)
    @SerialName("data")
    val data: Map<String, InnerSampleClass>?,
) {
    @Serializable
    data class InnerSampleClass(
        @SerialName("string")
        val string: String,
    )
}
