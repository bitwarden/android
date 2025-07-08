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
    object CipherNotFound : GetCipherResult()

    /**
     * Generic error while retrieving cipher. The optional [errorMessage] may be displayed directly
     * in the UI when present.
     */
    data class Error(
        val errorMessage: String?,
        val error: Throwable?,
    ) : GetCipherResult()
}
