package com.x8bit.bitwarden.data.vault.datasource.disk.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.x8bit.bitwarden.data.vault.datasource.disk.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

/**
 * Provides methods for inserting, retrieving, and deleting folders from the database using the
 * [FolderEntity].
 */
@Dao
interface FoldersDao {

    /**
     * Inserts multiple folders into the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<FolderEntity>)

    /**
     * Inserts a folder into the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    /**
     * Retrieves all folders from the database for a given [userId].
     */
    @Query("SELECT * FROM folders WHERE user_id = :userId")
    fun getAllFolders(
        userId: String,
    ): Flow<List<FolderEntity>>

    /**
     * Deletes all the stored folders associated with the given [userId]. This will return the
     * number of rows deleted by this query.
     */
    @Query("DELETE FROM folders WHERE user_id = :userId")
    suspend fun deleteAllFolders(userId: String): Int

    /**
     * Deletes the stored folder associated with the given [userId] that matches the [folderId].
     */
    @Query("DELETE FROM folders WHERE user_id = :userId AND id = :folderId")
    suspend fun deleteFolder(userId: String, folderId: String)

    /**
     * Deletes all the stored [folders] associated with the given [userId] and then add all new
     * `folders` to the database. This will return `true` if any changes were made to the database
     * and `false` otherwise.
     */
    @Transaction
    suspend fun replaceAllFolders(userId: String, folders: List<FolderEntity>): Boolean {
        val deletedFoldersCount = deleteAllFolders(userId)
        insertFolders(folders)
        return deletedFoldersCount > 0 || folders.isNotEmpty()
    }
}
