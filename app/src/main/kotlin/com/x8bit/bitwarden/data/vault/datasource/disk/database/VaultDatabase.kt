package com.x8bit.bitwarden.data.vault.datasource.disk.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import com.x8bit.bitwarden.data.vault.datasource.disk.convertor.InstantTypeConverter
import com.x8bit.bitwarden.data.vault.datasource.disk.dao.CiphersDao
import com.x8bit.bitwarden.data.vault.datasource.disk.dao.CollectionsDao
import com.x8bit.bitwarden.data.vault.datasource.disk.dao.DomainsDao
import com.x8bit.bitwarden.data.vault.datasource.disk.dao.FoldersDao
import com.x8bit.bitwarden.data.vault.datasource.disk.dao.SendsDao
import com.x8bit.bitwarden.data.vault.datasource.disk.entity.CipherEntity
import com.x8bit.bitwarden.data.vault.datasource.disk.entity.CollectionEntity
import com.x8bit.bitwarden.data.vault.datasource.disk.entity.DomainsEntity
import com.x8bit.bitwarden.data.vault.datasource.disk.entity.FolderEntity
import com.x8bit.bitwarden.data.vault.datasource.disk.entity.SendEntity

/**
 * Room database for storing any persisted data from the vault sync.
 */
@Database(
    entities = [
        CipherEntity::class,
        CollectionEntity::class,
        DomainsEntity::class,
        FolderEntity::class,
        SendEntity::class,
    ],
    version = 10,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10, RemoveTotpAutoMigration::class),
    ],
)
@TypeConverters(InstantTypeConverter::class)
abstract class VaultDatabase : RoomDatabase() {

    /**
     * Provides the DAO for accessing cipher data.
     */
    abstract fun cipherDao(): CiphersDao

    /**
     * Provides the DAO for accessing collection data.
     */
    abstract fun collectionDao(): CollectionsDao

    /**
     * Provides the DAO for accessing domains data.
     */
    abstract fun domainsDao(): DomainsDao

    /**
     * Provides the DAO for accessing folder data.
     */
    abstract fun folderDao(): FoldersDao

    /**
     * Provides the DAO for accessing send data.
     */
    abstract fun sendsDao(): SendsDao
}

/**
 * A defined migration to remove the `has_totp` field from the `cipher` table.
 */
@DeleteColumn.Entries(DeleteColumn(tableName = "ciphers", columnName = "has_totp"))
class RemoveTotpAutoMigration : AutoMigrationSpec
