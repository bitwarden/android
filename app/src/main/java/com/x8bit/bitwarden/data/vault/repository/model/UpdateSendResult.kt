package com.x8bit.bitwarden.data.vault.repository.model

/**
 * Models result of updating a send.
 */
sealed class UpdateSendResult {

    /**
     * Send updated successfully.
     */
    data object Success : UpdateSendResult()

    /**
     * Generic error while updating a send. The optional [errorMessage] may be displayed directly
     * in the UI when present.
     */
    data class Error(val errorMessage: String?) : UpdateSendResult()
}
