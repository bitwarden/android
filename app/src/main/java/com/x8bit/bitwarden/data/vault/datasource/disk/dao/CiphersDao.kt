package com.x8bit.bitwarden.data.vault.datasource.disk.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.x8bit.bitwarden.data.vault.datasource.disk.entity.CipherEntity
import kotlinx.coroutines.flow.Flow

/**
 * Provides methods for inserting, retrieving, and deleting ciphers from the database using the
 * [CipherEntity].
 */
@Dao
interface CiphersDao {

    /**
     * Inserts multiple ciphers into the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCiphers(ciphers: List<CipherEntity>)

    /**
     * Retrieves all ciphers from the database for a given [userId].
     */
    @Query("SELECT * FROM ciphers WHERE user_id = :userId")
    fun getAllCiphers(
        userId: String,
    ): Flow<List<CipherEntity>>

    /**
     * Deletes all the stored ciphers associated with the given [userId]. This will return the
     * number of rows deleted by this query.
     */
    @Query("DELETE FROM ciphers WHERE user_id = :userId")
    suspend fun deleteAllCiphers(userId: String): Int

    /**
     * Deletes the specified cipher associated with the given [userId] and [cipherId]. This will
     * return the number of rows deleted by this query.
     */
    @Query("DELETE FROM ciphers WHERE user_id = :userId AND id = :cipherId")
    suspend fun deleteCipher(userId: String, cipherId: String): Int

    /**
     * Deletes all the stored ciphers associated with the given [userId] and then add all new
     * [ciphers] to the database. This will return `true` if any changes were made to the database
     * and `false` otherwise.
     */
    @Transaction
    suspend fun replaceAllCiphers(userId: String, ciphers: List<CipherEntity>): Boolean {
        val deletedCiphersCount = deleteAllCiphers(userId)
        insertCiphers(ciphers)
        return deletedCiphersCount > 0 || ciphers.isNotEmpty()
    }
}
