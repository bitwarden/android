package com.x8bit.bitwarden.data.autofill.processor

import com.x8bit.bitwarden.data.autofill.model.AutofillCipher
import com.x8bit.bitwarden.data.autofill.provider.AutofillCipherProvider
import com.x8bit.bitwarden.data.autofill.provider.AutofillCipherProviderImpl
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AutofillCipherProviderTest {
    private lateinit var autofillCipherProvider: AutofillCipherProvider

    @BeforeEach
    fun setup() {
        autofillCipherProvider = AutofillCipherProviderImpl()
    }

    @Test
    fun `getCardAutofillCiphers should return default list of card ciphers`() = runTest {
        // Test & Verify
        val actual = autofillCipherProvider.getCardAutofillCiphers()

        assertEquals(CARD_CIPHERS, actual)
    }

    @Test
    fun `getLoginAutofillCiphers should return default list of login ciphers`() = runTest {
        // Test & Verify
        val actual = autofillCipherProvider.getLoginAutofillCiphers(
            uri = URI,
        )

        assertEquals(LOGIN_CIPHERS, actual)
    }
}

private val CARD_CIPHERS = listOf(
    AutofillCipher.Card(
        cardholderName = "John",
        code = "123",
        expirationMonth = "January",
        expirationYear = "1999",
        name = "John",
        number = "1234567890",
        subtitle = "123...",
    ),
    AutofillCipher.Card(
        cardholderName = "Doe",
        code = "456",
        expirationMonth = "December",
        expirationYear = "2024",
        name = "Doe",
        number = "0987654321",
        subtitle = "098...",
    ),
)
private val LOGIN_CIPHERS = listOf(
    AutofillCipher.Login(
        name = "Bitwarden1",
        password = "password123",
        subtitle = "John-Bitwarden",
        username = "John-Bitwarden",
    ),
    AutofillCipher.Login(
        name = "Bitwarden2",
        password = "password123",
        subtitle = "Doe-Bitwarden",
        username = "Doe-Bitwarden",
    ),
)
private const val URI: String = "androidapp://com.x8bit.bitwarden"
