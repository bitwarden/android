package com.bitwarden.network.api

import com.bitwarden.network.model.FolderJsonRequest
import com.bitwarden.network.model.NetworkResult
import com.bitwarden.network.model.SyncResponseJson
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Defines raw calls under the /folders API with authentication applied.
 */
internal interface FoldersApi {

    /**
     * Create a folder.
     */
    @POST("folders")
    suspend fun createFolder(@Body body: FolderJsonRequest): NetworkResult<SyncResponseJson.Folder>

    /**
     * Gets a folder.
     */
    @GET("folders/{folderId}")
    suspend fun getFolder(
        @Path("folderId") folderId: String,
    ): NetworkResult<SyncResponseJson.Folder>

    /**
     * Updates a folder.
     */
    @PUT("folders/{folderId}")
    suspend fun updateFolder(
        @Path("folderId") folderId: String,
        @Body body: FolderJsonRequest,
    ): NetworkResult<SyncResponseJson.Folder>

    /**
     * Deletes a folder.
     */
    @DELETE("folders/{folderId}")
    suspend fun deleteFolder(@Path("folderId") folderId: String): NetworkResult<Unit>
}
