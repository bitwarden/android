package com.x8bit.bitwarden.data.vault.repository.model

import com.x8bit.bitwarden.data.platform.util.userFriendlyMessage

/**
 * Models result of unarchiving a cipher.
 */
sealed class UnarchiveCipherResult {

    /**
     * Cipher unarchived successfully.
     */
    data object Success : UnarchiveCipherResult()

    /**
     * Generic error while unarchiving a cipher. The optional [errorMessage] may be
     * displayed directly in the UI when present.
     */
    data class Error(
        val error: Throwable,
        val errorMessage: String? = error.userFriendlyMessage,
    ) : UnarchiveCipherResult()
}
