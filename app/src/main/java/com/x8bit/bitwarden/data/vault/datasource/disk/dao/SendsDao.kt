package com.x8bit.bitwarden.data.vault.datasource.disk.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.x8bit.bitwarden.data.vault.datasource.disk.entity.SendEntity
import kotlinx.coroutines.flow.Flow

/**
 * Provides methods for inserting, retrieving, and deleting sends from the database using the
 * [SendEntity].
 */
@Dao
interface SendsDao {

    /**
     * Inserts multiple sends into the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSends(sends: List<SendEntity>)

    /**
     * Retrieves all sends from the database for a given [userId].
     */
    @Query("SELECT * FROM sends WHERE user_id = :userId")
    fun getAllSends(
        userId: String,
    ): Flow<List<SendEntity>>

    /**
     * Deletes the specified send associated with the given [userId] and [sendId]. This will return
     * the number of rows deleted by this query.
     */
    @Query("DELETE FROM sends WHERE user_id = :userId AND id = :sendId")
    suspend fun deleteSend(userId: String, sendId: String): Int

    /**
     * Deletes all the stored sends associated with the given [userId]. This will return the
     * number of rows deleted by this query.
     */
    @Query("DELETE FROM sends WHERE user_id = :userId")
    suspend fun deleteAllSends(userId: String): Int

    /**
     * Deletes all the stored sends associated with the given [userId] and then add all new
     * [sends] to the database. This will return `true` if any changes were made to the database
     * and `false` otherwise.
     */
    @Transaction
    suspend fun replaceAllSends(userId: String, sends: List<SendEntity>): Boolean {
        val deletedSendsCount = deleteAllSends(userId)
        insertSends(sends)
        return deletedSendsCount > 0 || sends.isNotEmpty()
    }
}
