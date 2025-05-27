package com.x8bit.bitwarden.data.vault.repository.model

/**
 * Models result of updating a cipher.
 */
sealed class UpdateCipherResult {

    /**
     * Cipher updated successfully.
     */
    data object Success : UpdateCipherResult()

    /**
     * Generic error while updating cipher. The optional [errorMessage] may be displayed directly in
     * the UI when present.
     */
    data class Error(val errorMessage: String?, val error: Throwable?) : UpdateCipherResult()
}
