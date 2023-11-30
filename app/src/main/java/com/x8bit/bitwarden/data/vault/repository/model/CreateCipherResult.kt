package com.x8bit.bitwarden.data.vault.repository.model

/**
 * Models result of creating a cipher.
 */
sealed class CreateCipherResult {

    /**
     * Cipher created successfully.
     */
    data object Success : CreateCipherResult()

    /**
     * Generic error while creating cipher.
     */
    data object Error : CreateCipherResult()
}
