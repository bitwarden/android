package com.x8bit.bitwarden.data.vault.repository.model

import com.x8bit.bitwarden.data.platform.util.userFriendlyMessage

/**
 * Models result of creating a cipher.
 */
sealed class CreateCipherResult {

    /**
     * Cipher created successfully.
     *
     * @property cipherId The server-assigned ID of the newly created cipher.
     */
    data class Success(
        val cipherId: String,
    ) : CreateCipherResult()

    /**
     * Generic error while creating cipher. The optional [errorMessage] may be displayed directly in
     * the UI when present.
     */
    data class Error(
        val error: Throwable?,
        val errorMessage: String? = error?.userFriendlyMessage,
    ) : CreateCipherResult()
}
