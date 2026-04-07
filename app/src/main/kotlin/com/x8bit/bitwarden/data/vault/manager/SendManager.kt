package com.x8bit.bitwarden.data.vault.manager

import android.net.Uri
import com.bitwarden.send.SendType
import com.bitwarden.send.SendView
import com.x8bit.bitwarden.data.vault.repository.model.CreateSendResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteSendResult
import com.x8bit.bitwarden.data.vault.repository.model.RemovePasswordSendResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateSendResult

/**
 * Manages the creating, updating, and deleting sends.
 */
interface SendManager {
    /**
     * Attempt to create a send. The [fileUri] _must_ be present when the given [SendView] has a
     * [SendView.type] of [SendType.FILE].
     */
    suspend fun createSend(sendView: SendView, fileUri: Uri?): CreateSendResult

    /**
     * Attempt to delete a send.
     */
    suspend fun deleteSend(sendId: String): DeleteSendResult

    /**
     * Attempt to remove the password from a send.
     */
    suspend fun removePasswordSend(sendId: String): RemovePasswordSendResult

    /**
     * Attempt to update a send.
     */
    suspend fun updateSend(
        sendId: String,
        sendView: SendView,
    ): UpdateSendResult
}
