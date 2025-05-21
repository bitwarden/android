package com.x8bit.bitwarden.data.autofill.provider

import com.x8bit.bitwarden.data.autofill.model.AutofillCipher

/**
 * A service for getting [AutofillCipher]s.
 */
interface AutofillCipherProvider {
    /**
     * Returns `true` if the vault for the current user is locked. This suspends in order to return
     * a value only after any unlocking vaults have fully unlocked (or failed to do so).
     */
    suspend fun isVaultLocked(): Boolean

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
