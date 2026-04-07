package com.x8bit.bitwarden.data.vault.repository.model

import com.x8bit.bitwarden.data.platform.util.userFriendlyMessage

/**
 * Models result of archiving a cipher.
 */
sealed class ArchiveCipherResult {

    /**
     * Cipher archived successfully.
     */
    data object Success : ArchiveCipherResult()

    /**
     * Generic error while archiving a cipher. The optional [errorMessage] may be displayed
     * directly in the UI when present.
     */
    data class Error(
        val error: Throwable,
        val errorMessage: String? = error.userFriendlyMessage,
    ) : ArchiveCipherResult()
}
