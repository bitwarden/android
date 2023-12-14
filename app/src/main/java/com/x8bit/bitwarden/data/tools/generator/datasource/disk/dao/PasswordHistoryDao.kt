package com.x8bit.bitwarden.data.tools.generator.datasource.disk.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.x8bit.bitwarden.data.tools.generator.datasource.disk.entity.PasswordHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Provides methods for inserting, retrieving, and deleting passcode history items
 * from the database, interacting with the [PasswordHistoryEntity] entity.
 */
@Dao
interface PasswordHistoryDao {

    /**
     * Inserts a password history item into the database.
     */
    @Insert
    suspend fun insertPasswordHistory(passwordHistory: PasswordHistoryEntity)

    /**
     * Retrieves all password history items for a specific user from the database as a Flow.
     */
    @Query("SELECT * FROM password_history WHERE userId = :userId")
    fun getPasswordHistoriesForUserAsFlow(userId: String): Flow<List<PasswordHistoryEntity>>

    /**
     * Clears all password history items from the database for a specific user.
     */
    @Query("DELETE FROM password_history WHERE userId = :userId")
    suspend fun clearPasswordHistoriesForUser(userId: String)
}
