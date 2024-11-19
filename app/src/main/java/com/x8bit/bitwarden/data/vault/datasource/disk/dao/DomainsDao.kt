package com.x8bit.bitwarden.data.vault.datasource.disk.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.x8bit.bitwarden.data.vault.datasource.disk.entity.DomainsEntity
import kotlinx.coroutines.flow.Flow

/**
 * Provides methods for inserting, retrieving, and deleting domains from the database using the
 * [DomainsEntity].
 */
@Dao
interface DomainsDao {
    /**
     * Deletes the stored domains associated with the given [userId].
     */
    @Query("DELETE FROM domains WHERE user_id = :userId")
    suspend fun deleteDomains(userId: String)

    /**
     * Retrieves domains from the database for a given [userId].
     */
    @Query("SELECT * FROM domains WHERE user_id = :userId")
    fun getDomains(
        userId: String,
    ): Flow<DomainsEntity?>

    /**
     * Inserts domains into the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDomains(domains: DomainsEntity)
}
