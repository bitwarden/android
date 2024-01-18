package com.x8bit.bitwarden.data.vault.repository.model

/**
 * Models result of sharing a cipher.
 */
sealed class ShareCipherResult {
    /**
     * Cipher shared successfully.
     */
    data object Success : ShareCipherResult()

    /**
     * Generic error while sharing cipher. The optional [errorMessage] may be displayed directly in
     * the UI when present.
     */
    data class Error(val errorMessage: String?) : ShareCipherResult()
}
