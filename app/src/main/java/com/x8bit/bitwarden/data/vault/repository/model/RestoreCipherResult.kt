package com.x8bit.bitwarden.data.vault.repository.model

/**
 * Models result of restoring a cipher.
 */
sealed class RestoreCipherResult {

    /**
     * Cipher restored successfully.
     */
    data object Success : RestoreCipherResult()

    /**
     * Generic error while restoring a cipher.
     */
    data object Error : RestoreCipherResult()
}
