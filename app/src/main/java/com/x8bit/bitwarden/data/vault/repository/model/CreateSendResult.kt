package com.x8bit.bitwarden.data.vault.repository.model

/**
 * Models result of creating a send.
 */
sealed class CreateSendResult {

    /**
     * send created successfully.
     */
    data object Success : CreateSendResult()

    /**
     * Generic error while creating a send.
     */
    data object Error : CreateSendResult()
}
