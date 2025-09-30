package com.x8bit.bitwarden.data.vault.repository.util

import com.bitwarden.collections.Collection
import com.bitwarden.collections.CollectionType
import com.bitwarden.network.model.CollectionTypeJson
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.model.createMockOrganization
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCollectionView
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

class VaultSdkCollectionExtensionsTest {
    @Test
    fun `toEncryptedSdkCollection should convert a network Collection to an SDK Collection`() {
        assertEquals(
            Collection(
                organizationId = "organizationId",
                hidePasswords = true,
                name = "name",
                externalId = "externalId",
                readOnly = true,
                id = "id",
                manage = true,
                defaultUserCollectionEmail = null,
                type = CollectionType.SHARED_COLLECTION,
            ),
            SyncResponseJson.Collection(
                organizationId = "organizationId",
                shouldHidePasswords = true,
                name = "name",
                externalId = "externalId",
                isReadOnly = true,
                id = "id",
                canManage = true,
                defaultUserCollectionEmail = null,
                type = CollectionTypeJson.SHARED_COLLECTION,
            )
                .toEncryptedSdkCollection(),
        )
    }

    @Test
    fun `toEncryptedSdkCollection should default manage to !isReadOnly if canManage is null`() {
        assertEquals(
            Collection(
                organizationId = "organizationId",
                hidePasswords = true,
                name = "name",
                externalId = "externalId",
                readOnly = false,
                id = "id",
                manage = true,
                defaultUserCollectionEmail = null,
                type = CollectionType.SHARED_COLLECTION,
            ),
            SyncResponseJson.Collection(
                organizationId = "organizationId",
                shouldHidePasswords = true,
                name = "name",
                externalId = "externalId",
                isReadOnly = false,
                id = "id",
                canManage = null,
                defaultUserCollectionEmail = null,
                type = CollectionTypeJson.SHARED_COLLECTION,
            )
                .toEncryptedSdkCollection(),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toEncryptedSdkCollectionList should convert a list of network Collections to a list of SDK Collections`() {
        assertEquals(
            listOf(
                Collection(
                    organizationId = "organizationId",
                    hidePasswords = true,
                    name = "name",
                    externalId = "externalId",
                    readOnly = true,
                    id = "id",
                    manage = true,
                    defaultUserCollectionEmail = null,
                    type = CollectionType.SHARED_COLLECTION,
                ),
            ),
            listOf(
                SyncResponseJson.Collection(
                    organizationId = "organizationId",
                    shouldHidePasswords = true,
                    name = "name",
                    externalId = "externalId",
                    isReadOnly = true,
                    id = "id",
                    canManage = true,
                    defaultUserCollectionEmail = null,
                    type = CollectionTypeJson.SHARED_COLLECTION,
                )
                    .toEncryptedSdkCollection(),
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toSortAlphabeticallyByTypeAndOrganization should sort collections by type and name`() {
        val list = listOf(
            createMockCollectionView(1).copy(name = "c"),
            createMockCollectionView(1).copy(name = "B"),
            createMockCollectionView(1).copy(name = "z"),
            createMockCollectionView(1).copy(name = "4"),
            createMockCollectionView(1).copy(name = "A"),
            createMockCollectionView(1).copy(name = "#"),
            createMockCollectionView(2).copy(
                name = "Org2 items",
                type = CollectionType.DEFAULT_USER_COLLECTION,
                organizationId = "mockId-2",
            ),
            createMockCollectionView(1).copy(
                name = "Org1 items",
                type = CollectionType.DEFAULT_USER_COLLECTION,
                organizationId = "mockId-1",
            ),
        )

        val expected = listOf(
            createMockCollectionView(1).copy(
                name = "Org1 items",
                type = CollectionType.DEFAULT_USER_COLLECTION,
                organizationId = "mockId-1",
            ),
            createMockCollectionView(2).copy(
                name = "Org2 items",
                type = CollectionType.DEFAULT_USER_COLLECTION,
                organizationId = "mockId-2",
            ),
            createMockCollectionView(1).copy(name = "#"),
            createMockCollectionView(1).copy(name = "4"),
            createMockCollectionView(1).copy(name = "A"),
            createMockCollectionView(1).copy(name = "B"),
            createMockCollectionView(1).copy(name = "c"),
            createMockCollectionView(1).copy(name = "z"),
        )

        assertEquals(
            expected,
            list.sortAlphabeticallyByTypeAndOrganization(
                userOrganizations = listOf(
                    createMockOrganization(number = 1),
                    createMockOrganization(number = 2),
                ),
            ),
        )
    }

    @Test
    fun `toCollectionType should convert CollectionTypeJson to CollectionType`() {
        val collectionType = CollectionTypeJson.SHARED_COLLECTION
        val sdkCollectionType = collectionType.toSdkCollectionType()
        assertEquals(
            CollectionType.SHARED_COLLECTION,
            sdkCollectionType,
        )
    }
}
