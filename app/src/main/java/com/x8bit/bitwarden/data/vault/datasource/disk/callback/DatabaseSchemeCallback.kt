package com.x8bit.bitwarden.data.vault.datasource.disk.callback

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.x8bit.bitwarden.data.platform.manager.DatabaseSchemeManager

/**
 * A [RoomDatabase.Callback] for tracking database scheme changes.
 */
class DatabaseSchemeCallback(
    private val databaseSchemeManager: DatabaseSchemeManager,
) : RoomDatabase.Callback() {
    override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
        databaseSchemeManager.clearSyncState()
    }
}
