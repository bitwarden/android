package com.x8bit.bitwarden.data.vault.datasource.disk.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.x8bit.bitwarden.data.vault.datasource.disk.dao.CiphersDao
import com.x8bit.bitwarden.data.vault.datasource.disk.entity.CipherEntity

/**
 * Room database for storing any persisted data from the vault sync.
 */
@Database(
    entities = [
        CipherEntity::class,
    ],
    version = 1,
)
abstract class VaultDatabase : RoomDatabase() {

    /**
     * Provides the DAO for accessing cipher data.
     */
    abstract fun cipherDao(): CiphersDao
}
