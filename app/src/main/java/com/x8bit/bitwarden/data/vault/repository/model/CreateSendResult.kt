package com.x8bit.bitwarden.data.vault.repository.model

import com.bitwarden.send.SendView

/**
 * Models result of creating a send.
 */
sealed class CreateSendResult {

    /**
     * Send created successfully and contains the decrypted [SendView].
     */
    data class Success(val sendView: SendView) : CreateSendResult()

    /**
     * Generic error while creating a send.
     */
    data class Error(val message: String?) : CreateSendResult()
}
