package com.x8bit.bitwarden.data.credentials.datasource.disk.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.x8bit.bitwarden.data.credentials.datasource.disk.dao.PrivilegedAppDao
import com.x8bit.bitwarden.data.credentials.datasource.disk.entity.PrivilegedAppEntity

/**
 * Room database for storing privileged apps.
 */
@Database(
    entities = [PrivilegedAppEntity::class],
    version = 1,
)
abstract class PrivilegedAppDatabase : RoomDatabase() {

    /**
     * Provides the DAO for accessing privileged apps.
     */
    abstract fun privilegedAppDao(): PrivilegedAppDao
}
