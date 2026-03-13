package com.x8bit.bitwarden.data.vault.repository.model

import com.x8bit.bitwarden.data.platform.util.userFriendlyMessage

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
    data class Error(
        val error: Throwable?,
        val errorMessage: String? = error?.userFriendlyMessage,
    ) : UpdateCipherResult()
}
