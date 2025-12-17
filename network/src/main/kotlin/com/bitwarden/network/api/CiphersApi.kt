package com.bitwarden.network.api

import com.bitwarden.network.model.AttachmentJsonRequest
import com.bitwarden.network.model.AttachmentJsonResponse
import com.bitwarden.network.model.BulkShareCiphersJsonRequest
import com.bitwarden.network.model.CipherJsonRequest
import com.bitwarden.network.model.CipherMiniResponseJson
import com.bitwarden.network.model.CreateCipherInOrganizationJsonRequest
import com.bitwarden.network.model.ImportCiphersJsonRequest
import com.bitwarden.network.model.NetworkResult
import com.bitwarden.network.model.ShareCipherJsonRequest
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.model.UpdateCipherCollectionsJsonRequest
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Defines raw calls under the /ciphers API with authentication applied.
 */
@Suppress("TooManyFunctions")
internal interface CiphersApi {

    /**
     * Create a cipher.
     */
    @POST("ciphers")
    suspend fun createCipher(@Body body: CipherJsonRequest): NetworkResult<SyncResponseJson.Cipher>

    /**
     * Create a cipher that belongs to an organization.
     */
    @POST("ciphers/create")
    suspend fun createCipherInOrganization(
        @Body body: CreateCipherInOrganizationJsonRequest,
    ): NetworkResult<SyncResponseJson.Cipher>

    /**
     * Associates an attachment with a cipher.
     */
    @POST("ciphers/{cipherId}/attachment/v2")
    suspend fun createAttachment(
        @Path("cipherId") cipherId: String,
        @Body body: AttachmentJsonRequest,
    ): NetworkResult<AttachmentJsonResponse.Success>

    /**
     * Uploads the attachment associated with a cipher.
     */
    @POST("ciphers/{cipherId}/attachment/{attachmentId}")
    suspend fun uploadAttachment(
        @Path("cipherId") cipherId: String,
        @Path("attachmentId") attachmentId: String,
        @Body body: MultipartBody,
    ): NetworkResult<Unit>

    /**
     * Updates a cipher.
     */
    @PUT("ciphers/{cipherId}")
    suspend fun updateCipher(
        @Path("cipherId") cipherId: String,
        @Body body: CipherJsonRequest,
    ): NetworkResult<SyncResponseJson.Cipher>

    /**
     * Shares a cipher.
     */
    @PUT("ciphers/{cipherId}/share")
    suspend fun shareCipher(
        @Path("cipherId") cipherId: String,
        @Body body: ShareCipherJsonRequest,
    ): NetworkResult<SyncResponseJson.Cipher>

    /**
     * Shares multiple ciphers in bulk.
     */
    @PUT("ciphers/share")
    suspend fun bulkShareCiphers(
        @Body body: BulkShareCiphersJsonRequest,
    ): NetworkResult<List<CipherMiniResponseJson>>

    /**
     * Shares an attachment.
     */
    @POST("ciphers/{cipherId}/attachment/{attachmentId}/share")
    suspend fun shareAttachment(
        @Path("cipherId") cipherId: String,
        @Path("attachmentId") attachmentId: String,
        @Query("organizationId") organizationId: String?,
        @Body body: MultipartBody,
    ): NetworkResult<Unit>

    /**
     * Updates a cipher's collections.
     */
    @PUT("ciphers/{cipherId}/collections")
    suspend fun updateCipherCollections(
        @Path("cipherId") cipherId: String,
        @Body body: UpdateCipherCollectionsJsonRequest,
    ): NetworkResult<Unit>

    /**
     * Hard deletes a cipher.
     */
    @DELETE("ciphers/{cipherId}")
    suspend fun hardDeleteCipher(
        @Path("cipherId") cipherId: String,
    ): NetworkResult<Unit>

    /**
     * Soft deletes a cipher.
     */
    @PUT("ciphers/{cipherId}/delete")
    suspend fun softDeleteCipher(
        @Path("cipherId") cipherId: String,
    ): NetworkResult<Unit>

    /**
     * Deletes an attachment from a cipher.
     */
    @DELETE("ciphers/{cipherId}/attachment/{attachmentId}")
    suspend fun deleteCipherAttachment(
        @Path("cipherId") cipherId: String,
        @Path("attachmentId") attachmentId: String,
    ): NetworkResult<Unit>

    /**
     * Restores a cipher.
     */
    @PUT("ciphers/{cipherId}/restore")
    suspend fun restoreCipher(
        @Path("cipherId") cipherId: String,
    ): NetworkResult<SyncResponseJson.Cipher>

    /**
     * Gets a cipher.
     */
    @GET("ciphers/{cipherId}")
    suspend fun getCipher(
        @Path("cipherId") cipherId: String,
    ): NetworkResult<SyncResponseJson.Cipher>

    /**
     * Gets a cipher attachment.
     */
    @GET("ciphers/{cipherId}/attachment/{attachmentId}")
    suspend fun getCipherAttachment(
        @Path("cipherId") cipherId: String,
        @Path("attachmentId") attachmentId: String,
    ): NetworkResult<SyncResponseJson.Cipher.Attachment>

    /**
     * Indicates if the active user has unassigned ciphers.
     */
    @GET("ciphers/has-unassigned-ciphers")
    suspend fun hasUnassignedCiphers(): NetworkResult<Boolean>

    @POST("ciphers/import")
    suspend fun importCiphers(
        @Body body: ImportCiphersJsonRequest,
    ): NetworkResult<Unit>
}
