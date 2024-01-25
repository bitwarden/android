package com.x8bit.bitwarden.data.vault.datasource.network.service

import com.x8bit.bitwarden.data.vault.datasource.network.model.CipherJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.ShareCipherJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.UpdateCipherResponseJson

/**
 * Provides an API for querying ciphers endpoints.
 */
interface CiphersService {
    /**
     * Attempt to create a cipher.
     */
    suspend fun createCipher(body: CipherJsonRequest): Result<SyncResponseJson.Cipher>

    /**
     * Attempt to update a cipher.
     */
    suspend fun updateCipher(
        cipherId: String,
        body: CipherJsonRequest,
    ): Result<UpdateCipherResponseJson>

    /**
     * Attempt to share a cipher.
     */
    suspend fun shareCipher(
        cipherId: String,
        body: ShareCipherJsonRequest,
    ): Result<SyncResponseJson.Cipher>

    /**
     * Attempt to hard delete a cipher.
     */
    suspend fun hardDeleteCipher(cipherId: String): Result<Unit>

    /**
     * Attempt to soft delete a cipher.
     */
    suspend fun softDeleteCipher(cipherId: String): Result<Unit>

    /**
     * Attempt to delete an attachment from a cipher.
     */
    suspend fun deleteCipherAttachment(
        cipherId: String,
        attachmentId: String,
    ): Result<Unit>

    /**
     * Attempt to restore a cipher.
     */
    suspend fun restoreCipher(cipherId: String): Result<Unit>
}
