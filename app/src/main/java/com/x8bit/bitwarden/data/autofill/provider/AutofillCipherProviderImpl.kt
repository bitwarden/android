package com.x8bit.bitwarden.data.autofill.provider

import com.x8bit.bitwarden.data.autofill.model.AutofillCipher

/**
 * The default [AutofillCipherProvider] implementation. This service is used for getting currrent
 * [AutofillCipher]s.
 */
class AutofillCipherProviderImpl : AutofillCipherProvider {
    override suspend fun getCardAutofillCiphers(): List<AutofillCipher.Card> {
        // TODO: fulfill with real ciphers (BIT-1294)
        return cardCiphers
    }

    override suspend fun getLoginAutofillCiphers(
        uri: String,
    ): List<AutofillCipher.Login> {
        // TODO: fulfill with real ciphers (BIT-1294)
        return loginCiphers
    }
}

private val cardCiphers = listOf(
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
private val loginCiphers = listOf(
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
