package com.x8bit.bitwarden.data.platform.datasource.network.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BaseSurrogateSerializerTest {
    private val json = Json

    @Test
    fun `properly deserializes raw JSON to the external model`() {
        assertEquals(
            ExternalData(
                dataAsInt = 100,
            ),
            json.decodeFromString<ExternalData>(
                """
                {
                 "dataAsString": "100"
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
                  "dataAsString": "100"
                }
                """,
            ),
            json.encodeToJsonElement(
                ExternalData(
                    dataAsInt = 100,
                ),
            ),
        )
    }
}

@Serializable
private data class InternalData(
    val dataAsString: String,
)

@Serializable(TestSurrogateSerializer::class)
private data class ExternalData(
    val dataAsInt: Int,
)

private class TestSurrogateSerializer : BaseSurrogateSerializer<ExternalData, InternalData>() {
    override val surrogateSerializer: KSerializer<InternalData>
        get() = InternalData.serializer()

    override fun InternalData.toExternalType(): ExternalData =
        ExternalData(
            dataAsInt = this.dataAsString.toInt(),
        )

    override fun ExternalData.toSurrogateType(): InternalData =
        InternalData(
            dataAsString = this.dataAsInt.toString(),
        )
}
