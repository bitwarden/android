package com.bitwarden.ui.platform.feature.cardscanner.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CardDataParserImplTest {

    private val parser = CardDataParserImpl()

    @Test
    fun `parseCardData extracts valid card number with Luhn check`() {
        val text = "4111 1111 1111 1111 12/25"
        assertEquals(
            CardScanData(
                number = "4111111111111111",
                expirationMonth = "12",
                expirationYear = "2025",
                securityCode = null,
            ),
            parser.parseCardData(text),
        )
    }

    @Test
    fun `parseCardData rejects invalid card number failing Luhn`() {
        val text = "4111 1111 1111 1112 12/25"
        assertEquals(
            CardScanData(
                number = null,
                expirationMonth = "12",
                expirationYear = "2025",
                securityCode = null,
            ),
            parser.parseCardData(text),
        )
    }

    @Test
    fun `parseCardData extracts four digit expiration year`() {
        val text = "4111 1111 1111 1111 06/2028"
        assertEquals(
            CardScanData(
                number = "4111111111111111",
                expirationMonth = "06",
                expirationYear = "2028",
                securityCode = null,
            ),
            parser.parseCardData(text),
        )
    }

    @Test
    fun `parseCardData extracts standalone CVV not adjacent to other digits`() {
        val text = "4111 1111 1111 1111 12/25 CVV 789"
        assertEquals(
            CardScanData(
                number = "4111111111111111",
                expirationMonth = "12",
                expirationYear = "2025",
                securityCode = "789",
            ),
            parser.parseCardData(text),
        )
    }

    @Test
    fun `parseCardData filters out CVV candidates adjacent to other digits`() {
        val text = "4111 1111 1111 1111 12/25\n5551234567"
        assertEquals(
            CardScanData(
                number = "4111111111111111",
                expirationMonth = "12",
                expirationYear = "2025",
                securityCode = null,
            ),
            parser.parseCardData(text),
        )
    }

    @Test
    fun `parseCardData filters phone number fragments from CVV detection`() {
        val text = """
            4111 1111 1111 1111
            12/25
            Call 8005551234
        """.trimIndent()
        assertEquals(
            CardScanData(
                number = "4111111111111111",
                expirationMonth = "12",
                expirationYear = "2025",
                securityCode = null,
            ),
            parser.parseCardData(text),
        )
    }

    @Test
    fun `parseCardData does not pick up expiry digits as CVV`() {
        val text = "4111 1111 1111 1111 12/25"
        assertEquals(
            CardScanData(
                number = "4111111111111111",
                expirationMonth = "12",
                expirationYear = "2025",
                securityCode = null,
            ),
            parser.parseCardData(text),
        )
    }

    @Test
    fun `parseCardData extracts CVV when separated from phone number`() {
        val text = """
            4111 1111 1111 1111
            12/25
            5551234567
            456
        """.trimIndent()
        assertEquals(
            CardScanData(
                number = "4111111111111111",
                expirationMonth = "12",
                expirationYear = "2025",
                securityCode = "456",
            ),
            parser.parseCardData(text),
        )
    }

    @Test
    fun `parseCardData extracts four digit CVV for Amex`() {
        val text = "3782 822463 10005 09/26 1234"
        assertEquals(
            CardScanData(
                number = "378282246310005",
                expirationMonth = "09",
                expirationYear = "2026",
                securityCode = "1234",
            ),
            parser.parseCardData(text),
        )
    }

    @Test
    fun `parseCardData returns null when no card data is found`() {
        assertNull(parser.parseCardData("Hello world, no card here"))
    }

    @Test
    fun `parseCardData handles card number with dashes`() {
        val text = "4111-1111-1111-1111 03/27"
        assertEquals(
            CardScanData(
                number = "4111111111111111",
                expirationMonth = "03",
                expirationYear = "2027",
                securityCode = null,
            ),
            parser.parseCardData(text),
        )
    }

    @Test
    fun `parseCardData rejects four digit CVV for non-Amex card`() {
        val text = "4111 1111 1111 1111 12/25 1234"
        assertEquals(
            CardScanData(
                number = "4111111111111111",
                expirationMonth = "12",
                expirationYear = "2025",
                // Visa expects 3-digit CVV, so "1234" should not match
                securityCode = null,
            ),
            parser.parseCardData(text),
        )
    }

    @Test
    fun `parseCardData rejects three digit CVV for Amex card`() {
        val text = "3782 822463 10005 09/26 789"
        assertEquals(
            CardScanData(
                number = "378282246310005",
                expirationMonth = "09",
                expirationYear = "2026",
                // Amex expects 4-digit CID, so "789" should not match
                securityCode = null,
            ),
            parser.parseCardData(text),
        )
    }

    @Test
    fun `parseCardData extracts three digit CVV for Visa`() {
        val text = "4111 1111 1111 1111 12/25 CVV 321"
        assertEquals(
            CardScanData(
                number = "4111111111111111",
                expirationMonth = "12",
                expirationYear = "2025",
                securityCode = "321",
            ),
            parser.parseCardData(text),
        )
    }

    @Test
    fun `parseCardData returns CVV when card number not detected`() {
        // Without a valid card number, we can't determine brand,
        // so CVV detection falls back to 3-digit (non-Amex default)
        val text = "1234 5678 9012 3456 12/25 789"
        assertEquals(
            CardScanData(
                // Card number fails Luhn, so no brand detection possible
                number = null,
                expirationMonth = "12",
                expirationYear = "2025",
                securityCode = "789",
            ),
            parser.parseCardData(text),
        )
    }
}
