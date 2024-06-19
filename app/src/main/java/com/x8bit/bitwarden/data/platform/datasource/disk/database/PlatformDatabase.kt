package com.x8bit.bitwarden.data.platform.datasource.disk.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.x8bit.bitwarden.data.platform.datasource.disk.dao.OrganizationEventDao
import com.x8bit.bitwarden.data.platform.datasource.disk.entity.OrganizationEventEntity
import com.x8bit.bitwarden.data.vault.datasource.disk.convertor.ZonedDateTimeTypeConverter

/**
 * Room database for storing any persisted data for platform data.
 */
@Database(
    entities = [
        OrganizationEventEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(ZonedDateTimeTypeConverter::class)
abstract class PlatformDatabase : RoomDatabase() {
    /**
     * Provides the DAO for accessing organization event data.
     */
    abstract fun organizationEventDao(): OrganizationEventDao
}
