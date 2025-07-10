package com.x8bit.bitwarden.data.vault.manager.model

import com.bitwarden.vault.CipherView

/**
 * Models result of getting a cipher.
 */
sealed class GetCipherResult {
    /**
     * Cipher retrieved successfully.
     *
     * @param cipherView The cipher retrieved.
     */
    data class Success(
        val cipherView: CipherView,
    ) : GetCipherResult()

    /**
     * Cipher not found.
     */
    data object CipherNotFound : GetCipherResult()

    /**
     * Generic error while retrieving cipher.
     */
    data class Failure(
        val error: Throwable,
    ) : GetCipherResult()
}
