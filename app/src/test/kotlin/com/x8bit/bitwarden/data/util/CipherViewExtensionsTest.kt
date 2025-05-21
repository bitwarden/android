package com.x8bit.bitwarden.data.util

import com.bitwarden.vault.CardView
import com.bitwarden.vault.CipherType
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.IdentityView
import com.x8bit.bitwarden.data.platform.util.subtitle
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CipherViewExtensionsTest {

    @Test
    fun `subtitle should return empty string when type is LOGIN and login is null`() {
        // Setup
        val cipherView: CipherView = mockk {
            every { login } returns null
            every { type } returns CipherType.LOGIN
        }
        val expected = ""

        // Test
        val actual = cipherView.subtitle

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `subtitle should return username when type is LOGIN and it is username is non-null`() {
        // Setup
        val expected = "Bitwarden"
        val cipherView: CipherView = mockk {
            every { login?.username } returns expected
            every { type } returns CipherType.LOGIN
        }

        // Test
        val actual = cipherView.subtitle

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `subtitle should return empty string when type is LOGIN and username is null`() {
        // Setup
        val cipherView: CipherView = mockk {
            every { login?.username } returns null
            every { type } returns CipherType.LOGIN
        }
        val expected = ""

        // Test
        val actual = cipherView.subtitle

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `subtitle should return null when type is SECURE_NOTE`() {
        // Setup
        val cipherView: CipherView = mockk {
            every { type } returns CipherType.SECURE_NOTE
        }

        // Test
        val actual = cipherView.subtitle

        // Verify
        assertNull(actual)
    }

    @Test
    fun `subtitle should return null when type is CARD and card is null`() {
        // Setup
        val cipherView: CipherView = mockk {
            every { card } returns null
            every { type } returns CipherType.CARD
        }

        // Test
        val actual = cipherView.subtitle

        // Verify
        assertNull(actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `subtitle should return 4 digit display number when type is CARD, brand is blank, and number doesn't start with 34 or 37`() {
        // Setup
        val cardNumber = "4441233456789"
        val cardView: CardView = mockk {
            every { brand } returns " "
            every { number } returns cardNumber
        }
        val cipherView: CipherView = mockk {
            every { card } returns cardView
            every { type } returns CipherType.CARD
        }
        val expected = cardNumber.takeLast(4)

        // Test
        val actual = cipherView.subtitle

        // Verify
        assertEquals(expected, actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `subtitle should return 5 digit display number when type is CARD, brand is null, and number starts with 34`() {
        // Setup
        // Amex cards start with 34 or 37.
        val cardNumber = "341233456789"
        val cardView: CardView = mockk {
            every { brand } returns null
            every { number } returns cardNumber
        }
        val cipherView: CipherView = mockk {
            every { card } returns cardView
            every { type } returns CipherType.CARD
        }
        val expected = cardNumber.takeLast(5)

        // Test
        val actual = cipherView.subtitle

        // Verify
        assertEquals(expected, actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `subtitle should return 5 digit display number when type is CARD, brand is null, and number starts with 37`() {
        // Setup
        // Amex cards start with 34 or 37.
        val cardNumber = "371233456789"
        val cardView: CardView = mockk {
            every { brand } returns null
            every { number } returns cardNumber
        }
        val cipherView: CipherView = mockk {
            every { card } returns cardView
            every { type } returns CipherType.CARD
        }
        val expected = cardNumber.takeLast(5)

        // Test
        val actual = cipherView.subtitle

        // Verify
        assertEquals(expected, actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `subtitle should return 5 digit display number when type is CARD, brand is blank, and number starts with 37`() {
        // Setup
        // Amex cards start with 34 or 37.
        val cardNumber = "371233456789"
        val cardView: CardView = mockk {
            every { brand } returns " "
            every { number } returns cardNumber
        }
        val cipherView: CipherView = mockk {
            every { card } returns cardView
            every { type } returns CipherType.CARD
        }
        val expected = cardNumber.takeLast(5)

        // Test
        val actual = cipherView.subtitle

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `subtitle should return brand when type is CARD and number is null`() {
        // Setup
        val expected = "American Express"
        val cardView: CardView = mockk {
            every { brand } returns expected
            every { number } returns null
        }
        val cipherView: CipherView = mockk {
            every { card } returns cardView
            every { type } returns CipherType.CARD
        }

        // Test
        val actual = cipherView.subtitle

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `subtitle should return brand when type is CARD and number is blank`() {
        // Setup
        val expected = "American Express"
        val cardView: CardView = mockk {
            every { brand } returns expected
            every { number } returns " "
        }
        val cipherView: CipherView = mockk {
            every { card } returns cardView
            every { type } returns CipherType.CARD
        }

        // Test
        val actual = cipherView.subtitle

        // Verify
        assertEquals(expected, actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `subtitle should return brand and display number when type is CARD and brand and subtitle are populated`() {
        // Setup
        val expectedBrand = "American Express"
        val cardNumber = "4441233456789"
        val cardView: CardView = mockk {
            every { brand } returns expectedBrand
            every { number } returns cardNumber
        }
        val cipherView: CipherView = mockk {
            every { card } returns cardView
            every { type } returns CipherType.CARD
        }
        val shownCardDigits = cardNumber.takeLast(4)
        val expected = "$expectedBrand, *$shownCardDigits"

        // Test
        val actual = cipherView.subtitle

        // Verify
        assertEquals(expected, actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `subtitle should return first name when type is IDENTITY, first name is populated, and last name is null`() {
        // Setup
        val expected = "John"
        val identityView: IdentityView = mockk {
            every { firstName } returns expected
            every { lastName } returns null
        }
        val cipherView: CipherView = mockk {
            every { identity } returns identityView
            every { type } returns CipherType.IDENTITY
        }

        // Test
        val actual = cipherView.subtitle

        // Verify
        assertEquals(expected, actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `subtitle should return last name when type is IDENTITY, first name is null, and last name is populated`() {
        // Setup
        val expected = "Doe"
        val identityView: IdentityView = mockk {
            every { firstName } returns null
            every { lastName } returns expected
        }
        val cipherView: CipherView = mockk {
            every { identity } returns identityView
            every { type } returns CipherType.IDENTITY
        }

        // Test
        val actual = cipherView.subtitle

        // Verify
        assertEquals(expected, actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `subtitle should return first name when type is IDENTITY and first and last name are populated`() {
        // Setup
        val expectedFirstName = "John"
        val expectedLastName = "Doe"
        val identityView: IdentityView = mockk {
            every { firstName } returns expectedFirstName
            every { lastName } returns expectedLastName
        }
        val cipherView: CipherView = mockk {
            every { identity } returns identityView
            every { type } returns CipherType.IDENTITY
        }
        val expected = "$expectedFirstName $expectedLastName"

        // Test
        val actual = cipherView.subtitle

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `subtitle should return null when type is IDENTITY and identity is null`() {
        val cipherView: CipherView = mockk {
            every { identity } returns null
            every { type } returns CipherType.IDENTITY
        }

        val actual = cipherView.subtitle

        assertNull(actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `subtitle should return null when type is IDENTITY and first and last name are null`() {
        // Setup
        val identityView: IdentityView = mockk {
            every { firstName } returns null
            every { lastName } returns null
        }
        val cipherView: CipherView = mockk {
            every { identity } returns identityView
            every { type } returns CipherType.IDENTITY
        }

        // Test
        val actual = cipherView.subtitle

        // Verify
        assertNull(actual)
    }

    @Test
    fun `subtitle should return null when type is SSH_KEY`() {
        val cipherView: CipherView = mockk {
            every { type } returns CipherType.SSH_KEY
        }

        val actual = cipherView.subtitle

        assertNull(actual)
    }
}
