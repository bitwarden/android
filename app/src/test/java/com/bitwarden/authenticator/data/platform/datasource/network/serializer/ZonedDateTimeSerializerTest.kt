package com.bitwarden.authenticator.data.platform.datasource.network.serializer

import com.bitwarden.authenticator.data.platform.datasource.network.di.PlatformNetworkModule
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class ZonedDateTimeSerializerTest {
    private val json = PlatformNetworkModule.providesJson()

    @Test
    fun `properly deserializes raw JSON to ZonedDateTime`() {
        assertEquals(
            ZonedDateTimeData(
                dataAsZonedDateTime = ZonedDateTime.of(
                    2023,
                    10,
                    6,
                    17,
                    22,
                    28,
                    440000000,
                    ZoneOffset.UTC,
                ),
            ),
            json.decodeFromString<ZonedDateTimeData>(
                """
                {
                 "dataAsZonedDateTime": "2023-10-06T17:22:28.44Z"
                }
                """,
            ),
        )
    }

    @Test
    fun `properly deserializes raw JSON with nano seconds to ZonedDateTime`() {
        assertEquals(
            ZonedDateTimeData(
                dataAsZonedDateTime = ZonedDateTime.of(
                    2023,
                    8,
                    1,
                    16,
                    13,
                    3,
                    502391000,
                    ZoneOffset.UTC,
                ),
            ),
            json.decodeFromString<ZonedDateTimeData>(
                """
                {
                 "dataAsZonedDateTime": "2023-08-01T16:13:03.502391Z"
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
                  "dataAsZonedDateTime": "2023-10-06T17:22:28.440Z"
                }
                """,
            ),
            json.encodeToJsonElement(
                ZonedDateTimeData(
                    dataAsZonedDateTime = ZonedDateTime.of(
                        2023,
                        10,
                        6,
                        17,
                        22,
                        28,
                        440000000,
                        ZoneId.of("UTC"),
                    ),
                ),
            ),
        )
    }
}

@Serializable
private data class ZonedDateTimeData(
    @Serializable(ZonedDateTimeSerializer::class)
    @SerialName("dataAsZonedDateTime")
    val dataAsZonedDateTime: ZonedDateTime,
)
