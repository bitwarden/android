package com.x8bit.bitwarden.data.vault.repository.model

import com.x8bit.bitwarden.data.platform.util.userFriendlyMessage

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
    data class Error(
        val error: Throwable?,
        val errorMessage: String? = error?.userFriendlyMessage,
    ) : CreateCipherResult()
}
