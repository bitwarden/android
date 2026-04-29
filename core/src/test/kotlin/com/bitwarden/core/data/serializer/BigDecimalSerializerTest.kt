package com.bitwarden.core.data.serializer

import com.bitwarden.core.di.CoreModule
import io.mockk.mockk
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BigDecimalSerializerTest {
    private val json = CoreModule.providesJson(buildInfoManager = mockk(relaxed = true))

    @Test
    fun `deserializes JSON number with decimals without precision loss`() {
        assertEquals(
            BigDecimalData(amount = BigDecimal("3.85")),
            json.decodeFromString<BigDecimalData>(
                """
                {
                  "amount": 3.85
                }
                """,
            ),
        )
    }

    @Test
    fun `deserializes JSON integer zero`() {
        assertEquals(
            BigDecimalData(amount = BigDecimal("0")),
            json.decodeFromString<BigDecimalData>(
                """
                {
                  "amount": 0
                }
                """,
            ),
        )
    }

    @Test
    fun `deserializes high-precision JSON number without coercing through Double`() {
        assertEquals(
            BigDecimalData(amount = BigDecimal("0.123456789012345")),
            json.decodeFromString<BigDecimalData>(
                """
                {
                  "amount": 0.123456789012345
                }
                """,
            ),
        )
    }

    @Test
    fun `deserializes negative JSON number`() {
        assertEquals(
            BigDecimalData(amount = BigDecimal("-4.20")),
            json.decodeFromString<BigDecimalData>(
                """
                {
                  "amount": -4.20
                }
                """,
            ),
        )
    }

    @Test
    fun `serializes BigDecimal as unquoted JSON number literal`() {
        assertEquals(
            json.parseToJsonElement(
                """
                {
                  "amount": 19.80
                }
                """,
            ),
            json.encodeToJsonElement(
                BigDecimalData(amount = BigDecimal("19.80")),
            ),
        )
    }
}

@Serializable
private data class BigDecimalData(
    @Serializable(BigDecimalSerializer::class)
    @SerialName("amount")
    val amount: BigDecimal,
)
