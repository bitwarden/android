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
    @Query("SELECT * FROM ciphers WHERE user_id IS :userId")
    fun getAllCiphers(
        userId: String,
    ): Flow<List<CipherEntity>>

    /**
     * Deletes all the stored ciphers associated with the given [userId].
     */
    @Query("DELETE FROM ciphers WHERE user_id = :userId")
    suspend fun deleteAllCiphers(userId: String)

    /**
     * Deletes all the stored ciphers associated with the given [userId] and then add all new
     * [ciphers] to the database.
     */
    @Transaction
    suspend fun replaceAllCiphers(userId: String, ciphers: List<CipherEntity>) {
        deleteAllCiphers(userId)
        insertCiphers(ciphers)
    }
}
