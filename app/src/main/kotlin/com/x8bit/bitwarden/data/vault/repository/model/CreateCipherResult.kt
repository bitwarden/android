package com.x8bit.bitwarden.data.vault.repository.model

/**
 * Models result of creating a cipher.
 */
sealed class CreateCipherResult {

    /**
     * Cipher created successfully.
     */
    data object Success : CreateCipherResult()

    /**
     * Generic error while creating cipher. The optional [errorMessage] may be displayed directly in
     * the UI when present.
     */
    data class Error(val errorMessage: String?, val error: Throwable?) : CreateCipherResult()
}
