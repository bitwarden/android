package com.x8bit.bitwarden.data.vault.datasource.disk.dao

import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.vault.datasource.disk.entity.CollectionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FakeCollectionsDao : CollectionsDao {

    val storedCollections = mutableListOf<CollectionEntity>()

    var deleteCollectionCalled: Boolean = false
    var deleteCollectionsCalled: Boolean = false
    var insertCollectionCalled = false

    private val collectionsFlow = bufferedMutableSharedFlow<List<CollectionEntity>>(replay = 1)

    init {
        collectionsFlow.tryEmit(emptyList())
    }

    override suspend fun deleteAllCollections(userId: String): Int {
        deleteCollectionsCalled = true
        val count = storedCollections.count { it.userId == userId }
        storedCollections.removeAll { it.userId == userId }
        collectionsFlow.tryEmit(storedCollections.toList())
        return count
    }

    override suspend fun deleteCollection(userId: String, collectionId: String) {
        deleteCollectionCalled = true
        storedCollections.removeAll { it.userId == userId && it.id == collectionId }
        collectionsFlow.tryEmit(storedCollections.toList())
    }

    override fun getAllCollections(userId: String): Flow<List<CollectionEntity>> =
        collectionsFlow.map { ciphers -> ciphers.filter { it.userId == userId } }

    override suspend fun insertCollections(collections: List<CollectionEntity>) {
        storedCollections.addAll(collections)
        collectionsFlow.tryEmit(storedCollections.toList())
    }

    override suspend fun insertCollection(collection: CollectionEntity) {
        storedCollections.add(collection)
        collectionsFlow.tryEmit(storedCollections.toList())
        insertCollectionCalled = true
    }

    override suspend fun replaceAllCollections(
        userId: String,
        collections: List<CollectionEntity>,
    ): Boolean {
        val removed = storedCollections.removeAll { it.userId == userId }
        storedCollections.addAll(collections)
        collectionsFlow.tryEmit(storedCollections.toList())
        return removed || collections.isNotEmpty()
    }
}
