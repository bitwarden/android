package com.x8bit.bitwarden.data.vault.repository.model

import com.bitwarden.send.SendView

/**
 * Models result of removing the password protection from a send.
 */
sealed class RemovePasswordSendResult {

    /**
     * Send has had the password protection successfully removed and contains the decrypted
     * [SendView].
     */
    data class Success(val sendView: SendView) : RemovePasswordSendResult()

    /**
     * Generic error while removing the password protection from a send. The optional
     * [errorMessage] may be displayed directly in the UI when present.
     */
    data class Error(val errorMessage: String?) : RemovePasswordSendResult()
}
