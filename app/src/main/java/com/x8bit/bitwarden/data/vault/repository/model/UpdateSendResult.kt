package com.x8bit.bitwarden.data.vault.repository.model

import com.bitwarden.send.SendView

/**
 * Models result of updating a send.
 */
sealed class UpdateSendResult {

    /**
     * Send was updated successfully and contains the decrypted [SendView].
     */
    data class Success(val sendView: SendView) : UpdateSendResult()

    /**
     * Generic error while updating a send. The optional [errorMessage] may be displayed directly
     * in the UI when present.
     */
    data class Error(val errorMessage: String?) : UpdateSendResult()
}
