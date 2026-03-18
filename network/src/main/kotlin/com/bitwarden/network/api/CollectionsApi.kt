package com.bitwarden.network.api

import com.bitwarden.network.model.CollectionJsonRequest
import com.bitwarden.network.model.NetworkResult
import com.bitwarden.network.model.SyncResponseJson
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Defines raw calls under the /organizations/{orgId}/collections API with authentication applied.
 */
internal interface CollectionsApi {

    /**
     * Create a collection.
     */
    @POST("organizations/{orgId}/collections")
    suspend fun createCollection(
        @Path("orgId") organizationId: String,
        @Body body: CollectionJsonRequest,
    ): NetworkResult<SyncResponseJson.Collection>

    /**
     * Gets a collection.
     */
    @GET("organizations/{orgId}/collections/{collectionId}")
    suspend fun getCollection(
        @Path("orgId") organizationId: String,
        @Path("collectionId") collectionId: String,
    ): NetworkResult<SyncResponseJson.Collection>

    /**
     * Updates a collection.
     */
    @PUT("organizations/{orgId}/collections/{collectionId}")
    suspend fun updateCollection(
        @Path("orgId") organizationId: String,
        @Path("collectionId") collectionId: String,
        @Body body: CollectionJsonRequest,
    ): NetworkResult<SyncResponseJson.Collection>

    /**
     * Deletes a collection.
     */
    @DELETE("organizations/{orgId}/collections/{collectionId}")
    suspend fun deleteCollection(
        @Path("orgId") organizationId: String,
        @Path("collectionId") collectionId: String,
    ): NetworkResult<Unit>
}
