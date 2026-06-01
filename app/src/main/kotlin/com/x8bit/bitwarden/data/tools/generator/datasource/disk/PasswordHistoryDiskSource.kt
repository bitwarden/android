package com.x8bit.bitwarden.data.tools.generator.datasource.disk

import com.x8bit.bitwarden.data.tools.generator.datasource.disk.entity.PasswordHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Primary access point for disk information related to password history.
 */
interface PasswordHistoryDiskSource {

    /**
     * Retrieves all password history items from the data source as a Flow.
     */
    fun getPasswordHistoriesForUser(userId: String): Flow<List<PasswordHistoryEntity>>

    /**
     * Inserts a generated history item into the data source.
     */
    suspend fun insertPasswordHistory(passwordHistoryEntity: PasswordHistoryEntity)

    /**
     * Clears all password history items from the data source.
     */
    suspend fun clearPasswordHistories(userId: String)
}
