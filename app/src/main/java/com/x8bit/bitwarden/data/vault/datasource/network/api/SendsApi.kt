package com.x8bit.bitwarden.data.vault.datasource.network.api

import androidx.annotation.Keep
import com.x8bit.bitwarden.data.vault.datasource.network.model.SendJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Defines raw calls under the /send API with authentication applied.
 */
@Keep
interface SendsApi {

    /**
     * Create a send.
     */
    @POST("sends")
    suspend fun createSend(@Body body: SendJsonRequest): Result<SyncResponseJson.Send>

    /**
     * Updates a send.
     */
    @PUT("sends/{sendId}")
    suspend fun updateSend(
        @Path("sendId") sendId: String,
        @Body body: SendJsonRequest,
    ): Result<SyncResponseJson.Send>

    /**
     * Deletes a send.
     */
    @DELETE("sends/{sendId}")
    suspend fun deleteSend(@Path("sendId") sendId: String): Result<Unit>
}
