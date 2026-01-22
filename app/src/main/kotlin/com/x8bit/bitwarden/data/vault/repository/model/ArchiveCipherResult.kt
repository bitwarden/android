package com.x8bit.bitwarden.data.vault.repository.model

/**
 * Models result of archiving a cipher.
 */
sealed class ArchiveCipherResult {

    /**
     * Cipher archived successfully.
     */
    data object Success : ArchiveCipherResult()

    /**
     * Generic error while archiving a cipher.
     */
    data class Error(val error: Throwable) : ArchiveCipherResult()
}
