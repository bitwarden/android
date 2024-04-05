package com.x8bit.bitwarden.authenticator.data.authenticator.repository.model

/**
 * Models result of creating a cipher.
 */
sealed class CreateItemResult {

    /**
     * Cipher created successfully.
     */
    data object Success : CreateItemResult()

    /**
     * Generic error while creating cipher.
     */
    data object Error : CreateItemResult()
}
