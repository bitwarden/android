package com.bitwarden.core.data.serializer

import com.bitwarden.core.di.CoreModule
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class InstantSerializerTest {
    private val json = CoreModule.providesJson()

    @Test
    fun `properly deserializes raw JSON to Instant`() {
        assertEquals(
            InstantData(
                dataAsInstant = Instant.ofEpochSecond(1_696_612_948L, 440_000_000L),
            ),
            json.decodeFromString<InstantData>(
                """
                {
                 "dataAsInstant": "2023-10-06T17:22:28.44Z"
                }
                """,
            ),
        )
    }

    @Test
    fun `properly deserializes raw JSON with nano seconds to Instant`() {
        assertEquals(
            InstantData(
                dataAsInstant = Instant.ofEpochSecond(1_690_906_383L, 502_391_000L),
            ),
            json.decodeFromString<InstantData>(
                """
                {
                 "dataAsInstant": "2023-08-01T16:13:03.502391Z"
                }
                """,
            ),
        )
    }

    @Test
    fun `properly serializes external model back to raw JSON`() {
        assertEquals(
            json.parseToJsonElement(
                """
                {
                  "dataAsInstant": "2023-10-06T17:22:28.440Z"
                }
                """,
            ),
            json.encodeToJsonElement(
                InstantData(
                    dataAsInstant = Instant.ofEpochSecond(1_696_612_948L, 440_000_000L),
                ),
            ),
        )
    }
}

@Serializable
private data class InstantData(
    @Serializable(InstantSerializer::class)
    @SerialName("dataAsInstant")
    val dataAsInstant: Instant,
)
