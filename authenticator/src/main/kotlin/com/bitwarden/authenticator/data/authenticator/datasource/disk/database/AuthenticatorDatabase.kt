package com.bitwarden.authenticator.data.authenticator.datasource.disk.database

import androidx.room3.AutoMigration
import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.bitwarden.authenticator.data.authenticator.datasource.disk.convertor.AuthenticatorItemAlgorithmConverter
import com.bitwarden.authenticator.data.authenticator.datasource.disk.convertor.AuthenticatorItemTypeConverter
import com.bitwarden.authenticator.data.authenticator.datasource.disk.dao.ItemDao
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity

/**
 * Room database for storing any persisted data.
 */
@Database(
    entities = [
        AuthenticatorItemEntity::class,
    ],
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ],
    version = 2,
    exportSchema = true,
)
@ColumnTypeConverters(
    AuthenticatorItemTypeConverter::class,
    AuthenticatorItemAlgorithmConverter::class,
)
abstract class AuthenticatorDatabase : RoomDatabase() {

    /**
     * Provide the DAO for accessing authenticator item data.
     */
    abstract fun itemDao(): ItemDao
}
