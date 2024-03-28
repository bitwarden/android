package com.x8bit.bitwarden.authenticator.data.authenticator.repository.model

/**
 * Models result of deleting a cipher.
 */
sealed class DeleteItemResult {

    /**
     * Cipher deleted successfully.
     */
    data object Success : DeleteItemResult()

    /**
     * Generic error while deleting a cipher.
     */
    data object Error : DeleteItemResult()
}
