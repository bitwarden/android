package com.x8bit.bitwarden.ui.vault.feature.util

import com.bitwarden.vault.CollectionView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCollectionView
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CollectionViewExtensionsTest {

    @Test
    fun `getCollections should get the collections for a collectionId with the correct names`() {
        val collectionList: List<CollectionView> = listOf(
            createMockCollectionView(number = 1, name = "test"),
            createMockCollectionView(number = 2, name = "test/test"),
            createMockCollectionView(number = 3, name = "test/Collection"),
            createMockCollectionView(number = 4, name = "test/test/test"),
            createMockCollectionView(number = 5, name = "Collection"),
        )

        val expected = listOf(
            createMockCollectionView(number = 2, name = "test"),
            createMockCollectionView(number = 3, name = "Collection"),
        )

        assertEquals(
            expected,
            collectionList.getCollections("mockId-1"),
        )
    }

    @Test
    fun `getFilteredCollections should properly filter out sub collections in a list`() {
        val collectionList: List<CollectionView> = listOf(
            createMockCollectionView(number = 1, name = "test"),
            createMockCollectionView(number = 2, name = "test/test"),
            createMockCollectionView(number = 3, name = "test/Collection"),
            createMockCollectionView(number = 4, name = "test/test/test"),
            createMockCollectionView(number = 5, name = "Collection"),
        )

        val expected = listOf(
            createMockCollectionView(number = 1, name = "test"),
            createMockCollectionView(number = 5, name = "Collection"),
        )

        assertEquals(
            expected,
            collectionList.getFilteredCollections(),
        )
    }

    @Test
    fun `toCollectionDisplayName should return the correct name`() {
        val collectionName = "Collection/test/2"

        val collectionList: List<CollectionView> = listOf(
            createMockCollectionView(number = 1, name = "Collection/test"),
            createMockCollectionView(number = 2, name = "test/test"),
            createMockCollectionView(number = 3, name = "test/Collection"),
            createMockCollectionView(number = 4, name = collectionName),
            createMockCollectionView(number = 5, name = "Collection"),
        )

        assertEquals(
            "2",
            collectionName.toCollectionDisplayName(collectionList),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `hasDeletePermissionInAtLeastOneCollection should return true if the user has manage permission in at least one collection`() {
        val collectionList: List<CollectionView> = listOf(
            createMockCollectionView(number = 1, manage = true),
            createMockCollectionView(number = 2, manage = false),
        )

        val collectionIds = listOf("mockId-1", "mockId-2")

        assertTrue(collectionList.hasDeletePermissionInAtLeastOneCollection(collectionIds))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `hasDeletePermissionInAtLeastOneCollection should return false if the user does not have manage permission in at least one collection`() {
        val collectionList: List<CollectionView> = listOf(
            createMockCollectionView(number = 1, manage = false),
            createMockCollectionView(number = 2, manage = false),
        )
        val collectionIds = listOf("mockId-1", "mockId-2")
        assertFalse(collectionList.hasDeletePermissionInAtLeastOneCollection(collectionIds))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `hasDeletePermissionInAtLeastOneCollection should return true if the collectionView list is null`() {
        val collectionIds = listOf("mockId-1", "mockId-2")
        assertTrue(null.hasDeletePermissionInAtLeastOneCollection(collectionIds))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `hasDeletePermissionInAtLeastOneCollection should return true if the collectionIds list is null`() {
        val collectionList: List<CollectionView> = listOf(
            createMockCollectionView(number = 1, manage = true),
            createMockCollectionView(number = 2, manage = false),
        )
        assertTrue(collectionList.hasDeletePermissionInAtLeastOneCollection(null))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `hasDeletePermissionInAtLeastOneCollection should return true if the collectionIds list is empty`() {
        val collectionList: List<CollectionView> = listOf(
            createMockCollectionView(number = 1, manage = true),
            createMockCollectionView(number = 2, manage = false),
        )
        assertTrue(collectionList.hasDeletePermissionInAtLeastOneCollection(emptyList()))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `hasDeletePermissionInAtLeastOneCollection should return true if the collectionView list is empty`() {
        val collectionIds = listOf("mockId-1", "mockId-2")
        assertTrue(
            emptyList<CollectionView>().hasDeletePermissionInAtLeastOneCollection(
                collectionIds,
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `canAssociateToCollections should return true if the user has edit and manage permission`() {
        val collectionList: List<CollectionView> = listOf(
            createMockCollectionView(number = 1, manage = true, readOnly = false),
        )
        val collectionIds = listOf("mockId-1", "mockId-2")
        assertTrue(collectionList.canAssignToCollections(collectionIds))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `canAssociateToCollections should return false if the user does not have manage or edit permission in at least one collection`() {
        val collectionList: List<CollectionView> = listOf(
            createMockCollectionView(number = 1, manage = false, readOnly = true),
            createMockCollectionView(number = 2, manage = false, readOnly = true),
        )
        val collectionIds = listOf("mockId-1", "mockId-2")
        assertFalse(collectionList.canAssignToCollections(collectionIds))
    }

    @Test
    fun `canAssociateToCollections should return true if the collectionView list is null`() {
        val collectionIds = listOf("mockId-1", "mockId-2")
        assertTrue(null.canAssignToCollections(collectionIds))
    }

    @Test
    fun `canAssociateToCollections should return true if the collectionIds list is null`() {
        val collectionList: List<CollectionView> = listOf(
            createMockCollectionView(number = 1, manage = true, readOnly = false),
            createMockCollectionView(number = 2, manage = false),
        )
        assertTrue(collectionList.canAssignToCollections(null))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `hasEditPermissionInAtLeastOneCollection should return true if the item is in at least one non-readOnly collection`() {
        val collectionList: List<CollectionView> = listOf(
            createMockCollectionView(number = 1, readOnly = true),
            createMockCollectionView(number = 2, readOnly = false),
        )

        val collectionIds = listOf("mockId-1", "mockId-2")

        assertTrue(collectionList.hasEditPermissionInAtLeastOneCollection(collectionIds))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `hasEditPermissionInAtLeastOneCollection should return false if the item isn't in at least one non-readOnly collection`() {
        val collectionList: List<CollectionView> = listOf(
            createMockCollectionView(number = 1, readOnly = true),
            createMockCollectionView(number = 2, readOnly = true),
        )
        val collectionIds = listOf("mockId-1", "mockId-2")
        assertFalse(collectionList.hasEditPermissionInAtLeastOneCollection(collectionIds))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `hasEditPermissionInAtLeastOneCollection should return true if the collectionView list is null`() {
        val collectionIds = listOf("mockId-1", "mockId-2")
        assertTrue(null.hasEditPermissionInAtLeastOneCollection(collectionIds))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `hasEditPermissionInAtLeastOneCollection should return true if the collectionIds list is null`() {
        val collectionList: List<CollectionView> = listOf(
            createMockCollectionView(number = 1, readOnly = true),
            createMockCollectionView(number = 2, readOnly = false),
        )
        assertTrue(collectionList.hasEditPermissionInAtLeastOneCollection(null))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `hasEditPermissionInAtLeastOneCollection should return true if the collectionIds list is empty`() {
        val collectionList: List<CollectionView> = listOf(
            createMockCollectionView(number = 1, readOnly = true),
            createMockCollectionView(number = 2, readOnly = false),
        )
        assertTrue(collectionList.hasEditPermissionInAtLeastOneCollection(emptyList()))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `hasEditPermissionInAtLeastOneCollection should return true if the collectionView list is empty`() {
        val collectionIds = listOf("mockId-1", "mockId-2")
        assertTrue(
            emptyList<CollectionView>().hasEditPermissionInAtLeastOneCollection(
                collectionIds,
            ),
        )
    }
}
