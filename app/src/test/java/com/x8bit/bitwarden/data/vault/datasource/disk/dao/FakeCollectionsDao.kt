package com.x8bit.bitwarden.data.vault.datasource.disk.dao

import com.x8bit.bitwarden.data.vault.datasource.disk.entity.CollectionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map

class FakeCollectionsDao : CollectionsDao {

    val storedCollections = mutableListOf<CollectionEntity>()

    var deleteCollectionCalled: Boolean = false
    var deleteCollectionsCalled: Boolean = false

    private val collectionsFlow = MutableSharedFlow<List<CollectionEntity>>(
        replay = 1,
        extraBufferCapacity = Int.MAX_VALUE,
    )

    init {
        collectionsFlow.tryEmit(emptyList())
    }

    override suspend fun deleteAllCollections(userId: String) {
        deleteCollectionsCalled = true
        storedCollections.removeAll { it.userId == userId }
        collectionsFlow.tryEmit(storedCollections.toList())
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
    }

    override suspend fun replaceAllCollections(
        userId: String,
        collections: List<CollectionEntity>,
    ) {
        storedCollections.removeAll { it.userId == userId }
        storedCollections.addAll(collections)
        collectionsFlow.tryEmit(storedCollections.toList())
    }
}
