package com.x8bit.bitwarden.authenticator.data.authenticator.datasource.disk.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.x8bit.bitwarden.authenticator.data.authenticator.datasource.disk.convertor.AuthenticatorItemAlgorithmConverter
import com.x8bit.bitwarden.authenticator.data.authenticator.datasource.disk.convertor.AuthenticatorItemTypeConverter
import com.x8bit.bitwarden.authenticator.data.authenticator.datasource.disk.dao.ItemDao
import com.x8bit.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity

/**
 * Room database for storing any persisted data.
 */
@Database(
    entities = [
        AuthenticatorItemEntity::class
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(
    AuthenticatorItemTypeConverter::class,
    AuthenticatorItemAlgorithmConverter::class,
)
abstract class AuthenticatorDatabase : RoomDatabase() {

    /**
     * Provide the DAO for accessing authenticator item data.
     */
    abstract fun itemDao(): ItemDao

}
