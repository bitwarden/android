package com.x8bit.bitwarden.data.vault.repository.model

/**
 * Models result of updating a cipher.
 */
sealed class UpdateCipherResult {

    /**
     * Cipher updated successfully.
     */
    data object Success : UpdateCipherResult()

    /**
     * Generic error while updating cipher.
     */
    data object Error : UpdateCipherResult()
}
