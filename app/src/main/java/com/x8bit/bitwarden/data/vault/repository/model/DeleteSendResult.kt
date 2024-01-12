package com.x8bit.bitwarden.data.vault.repository.model

/**
 * Models result of deleting a send.
 */
sealed class DeleteSendResult {

    /**
     * Send delete successfully.
     */
    data object Success : DeleteSendResult()

    /**
     * Generic error while deleting a send.
     */
    data object Error : DeleteSendResult()
}
