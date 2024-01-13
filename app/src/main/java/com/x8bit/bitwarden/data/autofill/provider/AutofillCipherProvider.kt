package com.x8bit.bitwarden.data.autofill.provider

import com.x8bit.bitwarden.data.autofill.model.AutofillCipher

/**
 * A service for getting [AutofillCipher]s.
 */
interface AutofillCipherProvider {
    /**
     * Get all [AutofillCipher.Card]s for the current user.
     */
    suspend fun getCardAutofillCiphers(): List<AutofillCipher.Card>

    /**
     * Get all [AutofillCipher.Login]s for the current user.
     */
    suspend fun getLoginAutofillCiphers(
        uri: String,
    ): List<AutofillCipher.Login>
}
