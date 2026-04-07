package com.x8bit.bitwarden.data.vault.repository.model

import com.x8bit.bitwarden.data.platform.util.userFriendlyMessage

/**
 * Models result of sharing a cipher.
 */
sealed class ShareCipherResult {
    /**
     * Cipher shared successfully.
     */
    data object Success : ShareCipherResult()

    /**
     * Generic error while sharing cipher. The optional [errorMessage] may be displayed
     * directly in the UI when present.
     */
    data class Error(
        val error: Throwable,
        val errorMessage: String? = error.userFriendlyMessage,
    ) : ShareCipherResult()
}
