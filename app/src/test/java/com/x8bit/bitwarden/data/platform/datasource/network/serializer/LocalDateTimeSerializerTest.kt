package com.x8bit.bitwarden.data.platform.datasource.network.serializer

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.modules.SerializersModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class LocalDateTimeSerializerTest {
    private val json = Json {
        serializersModule = SerializersModule {
            contextual(LocalDateTime::class, LocalDateTimeSerializer())
        }
    }

    @Test
    fun `properly deserializes raw JSON to LocalDate`() {
        assertEquals(
            LocalDateTimeData(
                dataAsLocalDateTime = LocalDateTime.of(
                    2023,
                    10,
                    6,
                    17,
                    22,
                    28,
                    440000000,
                ),
            ),
            json.decodeFromString<LocalDateTimeData>(
                """
                {
                 "dataAsLocalDateTime": "2023-10-06T17:22:28.44Z"
                }
                """,
            ),
        )
    }

    @Test
    fun `properly deserializes raw JSON with nano seconds to LocalDate`() {
        assertEquals(
            LocalDateTimeData(
                dataAsLocalDateTime = LocalDateTime.of(
                    2023,
                    8,
                    1,
                    16,
                    13,
                    3,
                    502391000,
                ),
            ),
            json.decodeFromString<LocalDateTimeData>(
                """
                {
                 "dataAsLocalDateTime": "2023-08-01T16:13:03.502391Z"
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
                  "dataAsLocalDateTime": "2023-10-06T17:22:28.4400000Z"
                }
                """,
            ),
            json.encodeToJsonElement(
                LocalDateTimeData(
                    dataAsLocalDateTime = LocalDateTime.of(
                        2023,
                        10,
                        6,
                        17,
                        22,
                        28,
                        440000000,
                    ),
                ),
            ),
        )
    }
}

@Serializable
private data class LocalDateTimeData(
    @Serializable(LocalDateTimeSerializer::class)
    val dataAsLocalDateTime: LocalDateTime,
)
