package com.bitwarden.network.service

import com.bitwarden.network.api.CollectionsApi
import com.bitwarden.network.base.BaseServiceTest
import com.bitwarden.network.model.CollectionAccessSelectionJson
import com.bitwarden.network.model.CollectionDetailsResponseJson
import com.bitwarden.network.model.CollectionJsonRequest
import com.bitwarden.network.model.CollectionTypeJson
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.model.UpdateCollectionResponseJson
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import retrofit2.create

class CollectionServiceTest : BaseServiceTest() {
    private val collectionsApi: CollectionsApi = retrofit.create()

    private val collectionService: CollectionService = CollectionServiceImpl(
        collectionsApi = collectionsApi,
        json = json,
    )

    @Test
    fun `createCollection should return the correct response`() = runTest {
        server.enqueue(MockResponse().setBody(COLLECTION_SUCCESS_JSON))
        val result = collectionService.createCollection(
            organizationId = DEFAULT_ORG_ID,
            body = CollectionJsonRequest(name = DEFAULT_NAME),
        )
        assertEquals(DEFAULT_COLLECTION, result.getOrThrow())
    }

    @Test
    fun `getCollection should return the correct response`() = runTest {
        server.enqueue(MockResponse().setBody(COLLECTION_SUCCESS_JSON))
        val result = collectionService.getCollection(
            organizationId = DEFAULT_ORG_ID,
            collectionId = DEFAULT_ID,
        )
        assertEquals(DEFAULT_COLLECTION, result.getOrThrow())
    }

    @Test
    fun `getCollectionDetails should return the correct response`() =
        runTest {
            server.enqueue(
                MockResponse().setBody(COLLECTION_DETAILS_JSON),
            )
            val result = collectionService.getCollectionDetails(
                organizationId = DEFAULT_ORG_ID,
                collectionId = DEFAULT_ID,
            )
            assertEquals(DEFAULT_COLLECTION_DETAILS, result.getOrThrow())
        }

    @Test
    fun `updateCollection with success should return Success`() =
        runTest {
            server.enqueue(MockResponse().setBody(COLLECTION_SUCCESS_JSON))
            val result = collectionService.updateCollection(
                organizationId = DEFAULT_ORG_ID,
                collectionId = DEFAULT_ID,
                body = CollectionJsonRequest(name = DEFAULT_NAME),
            )
            assertEquals(
                UpdateCollectionResponseJson.Success(
                    collection = DEFAULT_COLLECTION,
                ),
                result.getOrThrow(),
            )
        }

    @Test
    fun `updateCollection with invalid response should return Invalid`() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(400)
                    .setBody(UPDATE_COLLECTION_INVALID_JSON),
            )
            val result = collectionService.updateCollection(
                organizationId = DEFAULT_ORG_ID,
                collectionId = DEFAULT_ID,
                body = CollectionJsonRequest(name = DEFAULT_NAME),
            )
            assertEquals(
                UpdateCollectionResponseJson.Invalid(
                    message = "At least one member or group must have can manage permission.",
                    validationErrors = null,
                ),
                result.getOrThrow(),
            )
        }

    @Test
    fun `deleteCollection should return success`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))
        val result = collectionService.deleteCollection(
            organizationId = DEFAULT_ORG_ID,
            collectionId = DEFAULT_ID,
        )
        assertEquals(Unit, result.getOrThrow())
    }
}

private const val DEFAULT_ID = "collectionId"
private const val DEFAULT_ORG_ID = "orgId"
private const val DEFAULT_NAME = "mockName"

private val DEFAULT_COLLECTION = SyncResponseJson.Collection(
    id = DEFAULT_ID,
    organizationId = DEFAULT_ORG_ID,
    name = DEFAULT_NAME,
    externalId = "externalId",
    shouldHidePasswords = false,
    isReadOnly = false,
    canManage = true,
    defaultUserCollectionEmail = null,
    type = CollectionTypeJson.SHARED_COLLECTION,
)

private val DEFAULT_COLLECTION_DETAILS = CollectionDetailsResponseJson(
    id = DEFAULT_ID,
    organizationId = DEFAULT_ORG_ID,
    name = DEFAULT_NAME,
    externalId = "externalId",
    groups = listOf(
        CollectionAccessSelectionJson(
            id = "groupId-1",
            readOnly = false,
            hidePasswords = false,
            manage = true,
        ),
    ),
    users = listOf(
        CollectionAccessSelectionJson(
            id = "userId-1",
            readOnly = false,
            hidePasswords = false,
            manage = true,
        ),
    ),
)

private const val COLLECTION_SUCCESS_JSON = """
{
  "id": "collectionId",
  "organizationId": "orgId",
  "name": "mockName",
  "externalId": "externalId",
  "hidePasswords": false,
  "readOnly": false,
  "manage": true,
  "type": 0
}
"""

private const val COLLECTION_DETAILS_JSON = """
{
  "id": "collectionId",
  "organizationId": "orgId",
  "name": "mockName",
  "externalId": "externalId",
  "groups": [
    {
      "id": "groupId-1",
      "readOnly": false,
      "hidePasswords": false,
      "manage": true
    }
  ],
  "users": [
    {
      "id": "userId-1",
      "readOnly": false,
      "hidePasswords": false,
      "manage": true
    }
  ]
}
"""

private const val UPDATE_COLLECTION_INVALID_JSON = """
{
  "message": "At least one member or group must have can manage permission.",
  "validationErrors": null
}
"""
