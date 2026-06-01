package com.x8bit.bitwarden.data.tools.generator.datasource.disk.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.x8bit.bitwarden.data.tools.generator.datasource.disk.dao.PasswordHistoryDao
import com.x8bit.bitwarden.data.tools.generator.datasource.disk.entity.PasswordHistoryEntity

/**
 * Room database for storing passcode history.
 */
@Database(
    entities = [PasswordHistoryEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class PasswordHistoryDatabase : RoomDatabase() {

    /**
     * Provides the DAO for accessing passcode history data.
     */
    abstract fun passwordHistoryDao(): PasswordHistoryDao
}
