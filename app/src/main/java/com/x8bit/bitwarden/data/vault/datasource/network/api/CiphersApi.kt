package com.x8bit.bitwarden.data.vault.datasource.network.api

import com.x8bit.bitwarden.data.vault.datasource.network.model.CipherJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.ShareCipherJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Defines raw calls under the /ciphers API with authentication applied.
 */
interface CiphersApi {

    /**
     * Create a cipher.
     */
    @POST("ciphers")
    suspend fun createCipher(@Body body: CipherJsonRequest): Result<SyncResponseJson.Cipher>

    /**
     * Updates a cipher.
     */
    @PUT("ciphers/{cipherId}")
    suspend fun updateCipher(
        @Path("cipherId") cipherId: String,
        @Body body: CipherJsonRequest,
    ): Result<SyncResponseJson.Cipher>

    /**
     * Shares a cipher.
     */
    @PUT("ciphers/{cipherId}/share")
    suspend fun shareCipher(
        @Path("cipherId") cipherId: String,
        @Body body: ShareCipherJsonRequest,
    ): Result<SyncResponseJson.Cipher>

    /**
     * Hard deletes a cipher.
     */
    @DELETE("ciphers/{cipherId}")
    suspend fun hardDeleteCipher(
        @Path("cipherId") cipherId: String,
    ): Result<Unit>

    /**
     * Soft deletes a cipher.
     */
    @PUT("ciphers/{cipherId}/delete")
    suspend fun softDeleteCipher(
        @Path("cipherId") cipherId: String,
    ): Result<Unit>

    /**
     * Deletes an attachment from a cipher.
     */
    @DELETE("ciphers/{cipherId}/attachment/{attachmentId}")
    suspend fun deleteCipherAttachment(
        @Path("cipherId") cipherId: String,
        @Path("attachmentId") attachmentId: String,
    ): Result<Unit>

    /**
     * Restores a cipher.
     */
    @PUT("ciphers/{cipherId}/restore")
    suspend fun restoreCipher(
        @Path("cipherId") cipherId: String,
    ): Result<Unit>
}
