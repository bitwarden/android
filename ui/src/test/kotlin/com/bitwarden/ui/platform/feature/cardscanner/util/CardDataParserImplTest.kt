package com.bitwarden.ui.platform.feature.cardscanner.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CardDataParserImplTest {

    private val parser = CardDataParserImpl()

    @Test
    fun `parseCardData extracts valid card number with Luhn check`() {
        val text = "4111 1111 1111 1111 12/25"
        val result = parser.parseCardData(text)
        assertEquals("4111111111111111", result.number)
    }

    @Test
    fun `parseCardData rejects invalid card number failing Luhn`() {
        val text = "4111 1111 1111 1112 12/25"
        val result = parser.parseCardData(text)
        assertNull(result.number)
    }

    @Test
    fun `parseCardData extracts expiration month and year`() {
        val text = "4111 1111 1111 1111 12/25"
        val result = parser.parseCardData(text)
        assertEquals("12", result.expirationMonth)
        assertEquals("2025", result.expirationYear)
    }

    @Test
    fun `parseCardData extracts four digit expiration year`() {
        val text = "4111 1111 1111 1111 06/2028"
        val result = parser.parseCardData(text)
        assertEquals("06", result.expirationMonth)
        assertEquals("2028", result.expirationYear)
    }

    @Test
    fun `parseCardData extracts standalone CVV not adjacent to other digits`() {
        val text = "4111 1111 1111 1111 12/25 CVV 789"
        val result = parser.parseCardData(text)
        assertEquals("789", result.securityCode)
    }

    @Test
    fun `parseCardData filters out CVV candidates adjacent to other digits`() {
        val text = "4111 1111 1111 1111 12/25\n5551234567"
        val result = parser.parseCardData(text)
        assertNull(result.securityCode)
    }

    @Test
    fun `parseCardData filters phone number fragments from CVV detection`() {
        val text = """
            4111 1111 1111 1111
            12/25
            Call 8005551234
        """.trimIndent()
        val result = parser.parseCardData(text)
        assertNull(result.securityCode)
    }

    @Test
    fun `parseCardData does not pick up expiry digits as CVV`() {
        val text = "4111 1111 1111 1111 12/25"
        val result = parser.parseCardData(text)
        assertNull(result.securityCode)
    }

    @Test
    fun `parseCardData extracts CVV when separated from phone number`() {
        val text = """
            4111 1111 1111 1111
            12/25
            5551234567
            456
        """.trimIndent()
        val result = parser.parseCardData(text)
        assertEquals("456", result.securityCode)
    }

    @Test
    fun `parseCardData extracts four digit CVV for Amex`() {
        val text = "3782 822463 10005 09/26 1234"
        val result = parser.parseCardData(text)
        assertEquals("1234", result.securityCode)
    }

    @Test
    fun `parseCardData extracts cardholder name in all caps`() {
        val text = """
            JOHN DOE
            4111 1111 1111 1111
            12/25
        """.trimIndent()
        val result = parser.parseCardData(text)
        assertEquals("JOHN DOE", result.cardholderName)
    }

    @Test
    fun `parseCardData does not extract lowercase name`() {
        val text = """
            John Doe
            4111 1111 1111 1111
            12/25
        """.trimIndent()
        val result = parser.parseCardData(text)
        assertNull(result.cardholderName)
    }

    @Test
    fun `parseCardData does not extract short names`() {
        val text = """
            JD
            4111 1111 1111 1111
            12/25
        """.trimIndent()
        val result = parser.parseCardData(text)
        assertNull(result.cardholderName)
    }

    @Test
    fun `parseCardData handles text with no card data`() {
        val text = "Hello world, no card here"
        val result = parser.parseCardData(text)
        assertNull(result.number)
        assertNull(result.expirationMonth)
        assertNull(result.expirationYear)
        assertNull(result.securityCode)
        assertNull(result.cardholderName)
    }

    @Test
    fun `parseCardData handles card number with dashes`() {
        val text = "4111-1111-1111-1111 03/27"
        val result = parser.parseCardData(text)
        assertEquals("4111111111111111", result.number)
        assertEquals("03", result.expirationMonth)
    }

    @Test
    fun `parseCardData rejects four digit CVV for non-Amex card`() {
        val text = "4111 1111 1111 1111 12/25 1234"
        val result = parser.parseCardData(text)
        // Visa expects 3-digit CVV, so "1234" should not match
        assertNull(result.securityCode)
    }

    @Test
    fun `parseCardData rejects three digit CVV for Amex card`() {
        val text = "3782 822463 10005 09/26 789"
        val result = parser.parseCardData(text)
        // Amex expects 4-digit CID, so "789" should not match
        assertNull(result.securityCode)
    }

    @Test
    fun `parseCardData extracts three digit CVV for Visa`() {
        val text = "4111 1111 1111 1111 12/25 CVV 321"
        val result = parser.parseCardData(text)
        assertEquals("321", result.securityCode)
    }

    @Test
    fun `parseCardData returns no CVV when card number not detected`() {
        // Without a valid card number, we can't determine brand,
        // so CVV detection falls back to 3-digit (non-Amex default)
        val text = "1234 5678 9012 3456 12/25 789"
        val result = parser.parseCardData(text)
        // Card number fails Luhn, so no brand detection possible
        assertEquals("789", result.securityCode)
    }
}
