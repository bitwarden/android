package com.bitwarden.network.service

import com.bitwarden.network.model.CollectionDetailsResponseJson
import com.bitwarden.network.model.CollectionJsonRequest
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.model.UpdateCollectionResponseJson

/**
 * Provides an API for querying collection endpoints.
 */
interface CollectionService {
    /**
     * Attempt to create a collection in the given organization.
     */
    suspend fun createCollection(
        organizationId: String,
        body: CollectionJsonRequest,
    ): Result<SyncResponseJson.Collection>

    /**
     * Attempt to update a collection in the given organization.
     */
    suspend fun updateCollection(
        organizationId: String,
        collectionId: String,
        body: CollectionJsonRequest,
    ): Result<UpdateCollectionResponseJson>

    /**
     * Attempt to hard delete a collection from the given organization.
     */
    suspend fun deleteCollection(
        organizationId: String,
        collectionId: String,
    ): Result<Unit>

    /**
     * Attempt to retrieve a collection from the given organization.
     */
    suspend fun getCollection(
        organizationId: String,
        collectionId: String,
    ): Result<SyncResponseJson.Collection>

    /**
     * Attempt to retrieve a collection with access details (groups and users)
     * from the given organization.
     */
    suspend fun getCollectionDetails(
        organizationId: String,
        collectionId: String,
    ): Result<CollectionDetailsResponseJson>
}
