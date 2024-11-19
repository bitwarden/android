package com.x8bit.bitwarden.data.vault.datasource.network.api

import androidx.annotation.Keep
import com.x8bit.bitwarden.data.platform.datasource.network.model.NetworkResult
import com.x8bit.bitwarden.data.vault.datasource.network.model.CreateFileSendResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.SendJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Defines raw calls under the /send API with authentication applied.
 */
@Keep
interface SendsApi {

    /**
     * Create a text send.
     */
    @POST("sends")
    suspend fun createTextSend(@Body body: SendJsonRequest): NetworkResult<SyncResponseJson.Send>

    /**
     * Create a file send.
     */
    @POST("sends/file/v2")
    suspend fun createFileSend(
        @Body body: SendJsonRequest,
    ): NetworkResult<CreateFileSendResponseJson>

    /**
     * Updates a send.
     */
    @PUT("sends/{sendId}")
    suspend fun updateSend(
        @Path("sendId") sendId: String,
        @Body body: SendJsonRequest,
    ): NetworkResult<SyncResponseJson.Send>

    /**
     * Uploads the file associated with a send.
     */
    @POST("sends/{sendId}/file/{fileId}")
    suspend fun uploadFile(
        @Path("sendId") sendId: String,
        @Path("fileId") fileId: String,
        @Body body: MultipartBody,
    ): NetworkResult<Unit>

    /**
     * Deletes a send.
     */
    @DELETE("sends/{sendId}")
    suspend fun deleteSend(@Path("sendId") sendId: String): NetworkResult<Unit>

    /**
     * Deletes a send.
     */
    @PUT("sends/{sendId}/remove-password")
    suspend fun removeSendPassword(
        @Path("sendId") sendId: String,
    ): NetworkResult<SyncResponseJson.Send>

    /**
     * Gets a send.
     */
    @GET("sends/{sendId}")
    suspend fun getSend(@Path("sendId") sendId: String): NetworkResult<SyncResponseJson.Send>
}
