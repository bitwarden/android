package com.x8bit.bitwarden.ui.vault.feature.util

import com.bitwarden.vault.CollectionView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCollectionView
import org.junit.jupiter.api.Assertions.assertEquals
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
}
