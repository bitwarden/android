package com.x8bit.bitwarden.data.vault.datasource.disk.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.x8bit.bitwarden.data.vault.datasource.disk.entity.CollectionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Provides methods for inserting, retrieving, and deleting collections from the database using the
 * [CollectionEntity].
 */
@Dao
interface CollectionsDao {

    /**
     * Inserts multiple collections into the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollections(collections: List<CollectionEntity>)

    /**
     * Inserts a collection into the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CollectionEntity)

    /**
     * Retrieves all collections from the database for a given [userId].
     */
    @Query("SELECT * FROM collections WHERE user_id = :userId")
    fun getAllCollections(userId: String): Flow<List<CollectionEntity>>

    /**
     * Deletes all the stored collections associated with the given [userId]. This will return the
     * number of rows deleted by this query.
     */
    @Query("DELETE FROM collections WHERE user_id = :userId")
    suspend fun deleteAllCollections(userId: String): Int

    /**
     * Deletes the stored collection associated with the given [userId] that matches the
     * [collectionId].
     */
    @Query("DELETE FROM collections WHERE user_id = :userId AND id = :collectionId")
    suspend fun deleteCollection(userId: String, collectionId: String)

    /**
     * Deletes all the stored [collections] associated with the given [userId] and then add all new
     * `collections` to the database. This will return `true` if any changes were made to the
     * database and `false` otherwise.
     *
     * @return `true` if any changes were made to the database.
     */
    @Transaction
    suspend fun replaceAllCollections(
        userId: String,
        collections: List<CollectionEntity>,
    ): Boolean {
        val deletedCollectionsCount = deleteAllCollections(userId)
        insertCollections(collections)
        return deletedCollectionsCount > 0 || collections.isNotEmpty()
    }
}
