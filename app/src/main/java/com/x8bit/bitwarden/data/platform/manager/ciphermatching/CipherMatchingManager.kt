package com.x8bit.bitwarden.data.platform.manager.ciphermatching

import com.bitwarden.vault.CipherView

/**
 * A manager for matching ciphers based on special criteria.
 */
interface CipherMatchingManager {
    /**
     * Filter [ciphers] for entries that match the [matchUri] in some fashion.
     */
    suspend fun filterCiphersForMatches(
        ciphers: List<CipherView>,
        matchUri: String,
    ): List<CipherView>
}
