package com.x8bit.bitwarden.data.platform.manager.ciphermatching

import com.bitwarden.vault.CipherListView

/**
 * A manager for matching ciphers based on special criteria.
 */
interface CipherMatchingManager {
    /**
     * Filter [cipherListViews] for entries that match the [matchUri] in some fashion.
     */
    suspend fun filterCiphersForMatches(
        cipherListViews: List<CipherListView>,
        matchUri: String,
    ): List<CipherListView>
}
