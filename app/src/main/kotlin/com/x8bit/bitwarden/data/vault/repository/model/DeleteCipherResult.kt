package com.x8bit.bitwarden.data.vault.repository.model

/**
 * Models result of deleting a cipher.
 */
sealed class DeleteCipherResult {

    /**
     * Cipher deleted successfully.
     */
    data object Success : DeleteCipherResult()

    /**
     * Generic error while deleting a cipher. The optional [errorMessage] may be displayed
     * directly in the UI when present.
     */
    data class Error(
        val error: Throwable,
        val errorMessage: String? = null,
    ) : DeleteCipherResult()
}
