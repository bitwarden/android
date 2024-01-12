package com.x8bit.bitwarden.data.vault.repository.model

/**
 * Models result of deleting a cipher.
 */
sealed class DeleteCipherResult {

    /**
     * Cipher deleted successfully.
     */
    data object Success : DeleteCipherResult()

    /**
     * Generic error while deleting a cipher.
     */
    data object Error : DeleteCipherResult()
}
