package com.x8bit.bitwarden.data.autofill.fido2.datasource.disk.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.x8bit.bitwarden.data.autofill.fido2.datasource.disk.dao.Fido2PrivilegedAppInfoDao
import com.x8bit.bitwarden.data.autofill.fido2.datasource.disk.entity.Fido2PrivilegedAppInfoEntity

/**
 * Room database for storing trusted FIDO2 privileged apps.
 */
@Database(
    entities = [
        Fido2PrivilegedAppInfoEntity::class,
    ],
    version = 1,
)
abstract class Fido2PrivilegedAppDatabase : RoomDatabase() {
    /**
     * Provides the DAO for accessing trusted FIDO2 privileged apps.
     */
    abstract fun fido2PrivilegedAppInfoDao(): Fido2PrivilegedAppInfoDao
}
