package com.bitwarden.network.service

import com.bitwarden.network.api.CollectionsApi
import com.bitwarden.network.model.CollectionDetailsResponseJson
import com.bitwarden.network.model.CollectionJsonRequest
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.model.UpdateCollectionResponseJson
import com.bitwarden.network.model.toBitwardenError
import com.bitwarden.network.util.NetworkErrorCode
import com.bitwarden.network.util.parseErrorBodyOrNull
import com.bitwarden.network.util.toResult
import kotlinx.serialization.json.Json

internal class CollectionServiceImpl(
    private val collectionsApi: CollectionsApi,
    private val json: Json,
) : CollectionService {
    override suspend fun createCollection(
        organizationId: String,
        body: CollectionJsonRequest,
    ): Result<SyncResponseJson.Collection> =
        collectionsApi
            .createCollection(organizationId = organizationId, body = body)
            .toResult()

    override suspend fun updateCollection(
        organizationId: String,
        collectionId: String,
        body: CollectionJsonRequest,
    ): Result<UpdateCollectionResponseJson> =
        collectionsApi
            .updateCollection(
                organizationId = organizationId,
                collectionId = collectionId,
                body = body,
            )
            .toResult()
            .map { UpdateCollectionResponseJson.Success(collection = it) }
            .recoverCatching { throwable ->
                throwable
                    .toBitwardenError()
                    .parseErrorBodyOrNull<UpdateCollectionResponseJson.Invalid>(
                        code = NetworkErrorCode.BAD_REQUEST,
                        json = json,
                    )
                    ?: throw throwable
            }

    override suspend fun deleteCollection(
        organizationId: String,
        collectionId: String,
    ): Result<Unit> =
        collectionsApi
            .deleteCollection(
                organizationId = organizationId,
                collectionId = collectionId,
            )
            .toResult()

    override suspend fun getCollection(
        organizationId: String,
        collectionId: String,
    ): Result<SyncResponseJson.Collection> =
        collectionsApi
            .getCollection(
                organizationId = organizationId,
                collectionId = collectionId,
            )
            .toResult()

    override suspend fun getCollectionDetails(
        organizationId: String,
        collectionId: String,
    ): Result<CollectionDetailsResponseJson> =
        collectionsApi
            .getCollectionDetails(
                organizationId = organizationId,
                collectionId = collectionId,
            )
            .toResult()
}
