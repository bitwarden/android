package com.x8bit.bitwarden.data.tools.generator.datasource.disk

import com.x8bit.bitwarden.data.tools.generator.datasource.disk.dao.PasswordHistoryDao
import com.x8bit.bitwarden.data.tools.generator.datasource.disk.entity.PasswordHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Primary implementation of [PasswordHistoryDiskSource].
 */
class PasswordHistoryDiskSourceImpl(
    private val passwordHistoryDao: PasswordHistoryDao,
) : PasswordHistoryDiskSource {

    override fun getPasswordHistoriesForUser(userId: String): Flow<List<PasswordHistoryEntity>> {
        return passwordHistoryDao.getPasswordHistoriesForUserAsFlow(userId)
    }

    override suspend fun insertPasswordHistory(
        passwordHistoryEntity: PasswordHistoryEntity,
    ) {
        passwordHistoryDao.insertPasswordHistory(passwordHistoryEntity)
    }

    override suspend fun clearPasswordHistories(userId: String) {
        passwordHistoryDao.clearPasswordHistoriesForUser(userId)
    }
}
