package com.bitwarden.core.data.serializer

import com.bitwarden.core.di.CoreModule
import io.mockk.mockk
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SafeListSerializerTest {
    private val json = CoreModule.providesJson(buildInfoManager = mockk(relaxed = true))

    @Test
    fun `deserializes JSON successfully with invalid items`() {
        assertEquals(
            TestClass(data = listOf(TestClass.InnerTestClass(string = "test"))),
            json.decodeFromString<TestClass>(
                """
                {
                  "data": [
                    { "string": "test" },
                    { "string": null }
                  ]
                }
                """,
            ),
        )
    }

    @Test
    fun `deserializes JSON successfully with null list`() {
        assertEquals(
            TestClass(data = null),
            json.decodeFromString<TestClass>(
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
                putJsonArray(key = "data") {
                    addJsonObject {
                        put(key = "string", value = "test")
                    }
                }
            },
            json.encodeToJsonElement(
                serializer = TestClass.serializer(),
                value = TestClass(data = listOf(TestClass.InnerTestClass(string = "test"))),
            ),
        )
    }
}

@Serializable
private data class TestClass(
    @Serializable(with = SafeListSerializer::class)
    @SerialName("data")
    val data: List<InnerTestClass>?,
) {
    @Serializable
    data class InnerTestClass(
        @SerialName("string")
        val string: String,
    )
}
