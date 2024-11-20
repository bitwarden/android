package com.x8bit.bitwarden.data.vault.repository.util

import com.bitwarden.vault.Collection
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
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
            ),
            SyncResponseJson.Collection(
                organizationId = "organizationId",
                shouldHidePasswords = true,
                name = "name",
                externalId = "externalId",
                isReadOnly = true,
                id = "id",
                canManage = true,
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
            ),
            SyncResponseJson.Collection(
                organizationId = "organizationId",
                shouldHidePasswords = true,
                name = "name",
                externalId = "externalId",
                isReadOnly = false,
                id = "id",
                canManage = null,
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
                )
                    .toEncryptedSdkCollection(),
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toSortAlphabetically should sort collections by name`() {
        val list = listOf(
            createMockCollectionView(1).copy(name = "c"),
            createMockCollectionView(1).copy(name = "B"),
            createMockCollectionView(1).copy(name = "z"),
            createMockCollectionView(1).copy(name = "4"),
            createMockCollectionView(1).copy(name = "A"),
            createMockCollectionView(1).copy(name = "#"),
            createMockCollectionView(1).copy(name = "D"),
        )

        val expected = listOf(
            createMockCollectionView(1).copy(name = "#"),
            createMockCollectionView(1).copy(name = "4"),
            createMockCollectionView(1).copy(name = "A"),
            createMockCollectionView(1).copy(name = "B"),
            createMockCollectionView(1).copy(name = "c"),
            createMockCollectionView(1).copy(name = "D"),
            createMockCollectionView(1).copy(name = "z"),
        )

        assertEquals(
            expected,
            list.sortAlphabetically(),
        )
    }
}
