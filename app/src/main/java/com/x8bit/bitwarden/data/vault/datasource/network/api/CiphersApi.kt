package com.x8bit.bitwarden.data.vault.datasource.network.api

import com.x8bit.bitwarden.data.vault.datasource.network.model.AttachmentJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.AttachmentJsonResponse
import com.x8bit.bitwarden.data.vault.datasource.network.model.CipherJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.CreateCipherInOrganizationJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.ShareCipherJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.UpdateCipherCollectionsJsonRequest
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Defines raw calls under the /ciphers API with authentication applied.
 */
@Suppress("TooManyFunctions")
interface CiphersApi {

    /**
     * Create a cipher.
     */
    @POST("ciphers")
    suspend fun createCipher(@Body body: CipherJsonRequest): Result<SyncResponseJson.Cipher>

    /**
     * Create a cipher that belongs to an organization.
     */
    @POST("ciphers/create")
    suspend fun createCipherInOrganization(
        @Body body: CreateCipherInOrganizationJsonRequest,
    ): Result<SyncResponseJson.Cipher>

    /**
     * Associates an attachment with a cipher.
     */
    @POST("ciphers/{cipherId}/attachment/v2")
    suspend fun createAttachment(
        @Path("cipherId") cipherId: String,
        @Body body: AttachmentJsonRequest,
    ): Result<AttachmentJsonResponse>

    /**
     * Uploads the attachment associated with a cipher.
     */
    @POST("ciphers/{cipherId}/attachment/{attachmentId}")
    suspend fun uploadAttachment(
        @Path("cipherId") cipherId: String,
        @Path("attachmentId") attachmentId: String,
        @Body body: MultipartBody,
    ): Result<Unit>

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
     * Updates a cipher's collections.
     */
    @PUT("ciphers/{cipherId}/collections")
    suspend fun updateCipherCollections(
        @Path("cipherId") cipherId: String,
        @Body body: UpdateCipherCollectionsJsonRequest,
    ): Result<Unit>

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

    /**
     * Gets a cipher.
     */
    @GET("ciphers/{cipherId}")
    suspend fun getCipher(
        @Path("cipherId") cipherId: String,
    ): Result<SyncResponseJson.Cipher>
}
