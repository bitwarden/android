package com.x8bit.bitwarden.data.platform.datasource.network.serializer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BaseEnumeratedIntSerializerTest {
    private val json = Json

    @Test
    fun `properly deserializes integers to enums`() {
        assertEquals(
            TestEnum.CASE_1,
            json.decodeFromString<TestEnum>(
                """
                1
                """,
            ),
        )
    }

    @Test
    fun `properly serializes enums back to integers`() {
        assertEquals(
            json.parseToJsonElement(
                """
                1
                """,
            ),
            json.encodeToJsonElement(
                TestEnum.CASE_1,
            ),
        )
    }

    @Test
    fun `properly returns default value when unknown value is provided`() {
        assertEquals(
            TestEnum.UNKNOWN,
            json.decodeFromString<TestEnum>(
                """
                -1
                """,
            ),
        )
    }
}

@Serializable(TestEnumSerializer::class)
private enum class TestEnum {
    @SerialName("1")
    CASE_1,

    @SerialName("2")
    CASE_2,

    @SerialName("-1")
    UNKNOWN,
}

private class TestEnumSerializer : BaseEnumeratedIntSerializer<TestEnum>(
    values = TestEnum.entries.toTypedArray(),
    default = TestEnum.UNKNOWN,
)
