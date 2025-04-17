package com.bitwarden.network.service

import com.bitwarden.network.model.CreateFileSendResponse
import com.bitwarden.network.model.CreateFileSendResponseJson
import com.bitwarden.network.model.CreateSendJsonResponse
import com.bitwarden.network.model.SendJsonRequest
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.model.UpdateSendResponseJson
import java.io.File

/**
 * Provides an API for querying sends endpoints.
 */
interface SendsService {
    /**
     * Attempt to create a text send.
     */
    suspend fun createTextSend(
        body: SendJsonRequest,
    ): Result<CreateSendJsonResponse>

    /**
     * Attempt to create a file send.
     */
    suspend fun createFileSend(
        body: SendJsonRequest,
    ): Result<CreateFileSendResponse>

    /**
     * Attempt to upload the given [encryptedFile] associated with the [sendFileResponse].
     */
    suspend fun uploadFile(
        sendFileResponse: CreateFileSendResponseJson,
        encryptedFile: File,
    ): Result<SyncResponseJson.Send>

    /**
     * Attempt to update a send.
     */
    suspend fun updateSend(
        sendId: String,
        body: SendJsonRequest,
    ): Result<UpdateSendResponseJson>

    /**
     * Attempt to delete a send.
     */
    suspend fun deleteSend(
        sendId: String,
    ): Result<Unit>

    /**
     * Attempt to remove password protection from a send.
     */
    suspend fun removeSendPassword(
        sendId: String,
    ): Result<UpdateSendResponseJson>

    /**
     * Attempt to retrieve a send.
     */
    suspend fun getSend(sendId: String): Result<SyncResponseJson.Send>
}
