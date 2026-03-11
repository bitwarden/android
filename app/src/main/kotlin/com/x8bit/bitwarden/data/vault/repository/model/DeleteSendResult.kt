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
     * Generic error while deleting a send. The optional [errorMessage] may be displayed
     * directly in the UI when present.
     */
    data class Error(
        val error: Throwable,
        val errorMessage: String? = null,
    ) : DeleteSendResult()
}
