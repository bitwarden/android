package com.x8bit.bitwarden.data.vault.manager

import com.bitwarden.collections.CollectionView
import com.x8bit.bitwarden.data.vault.repository.model.CreateCollectionResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteCollectionResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateCollectionResult

/**
 * Manages the creating, updating, and deleting collections.
 */
interface CollectionManager {
    /**
     * Attempt to create a collection in the given [organizationId].
     */
    suspend fun createCollection(
        organizationId: String,
        collectionView: CollectionView,
    ): CreateCollectionResult

    /**
     * Attempt to delete a collection.
     */
    suspend fun deleteCollection(
        organizationId: String,
        collectionId: String,
    ): DeleteCollectionResult

    /**
     * Attempt to update a collection.
     */
    suspend fun updateCollection(
        organizationId: String,
        collectionId: String,
        collectionView: CollectionView,
    ): UpdateCollectionResult
}
