package com.x8bit.bitwarden.data.vault.repository.model

/**
 * Models result of unarchiving a cipher.
 */
sealed class UnarchiveCipherResult {

    /**
     * Cipher unarchived successfully.
     */
    data object Success : UnarchiveCipherResult()

    /**
     * Generic error while archiving a cipher.
     */
    data object Error : UnarchiveCipherResult()
}
