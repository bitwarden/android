package com.x8bit.bitwarden.data.vault.datasource.disk.callback

import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteConnection
import com.x8bit.bitwarden.data.platform.manager.DatabaseSchemeManager

/**
 * A [RoomDatabase.Callback] for tracking database scheme changes.
 */
class DatabaseSchemeCallback(
    private val databaseSchemeManager: DatabaseSchemeManager,
) : RoomDatabase.Callback() {
    override suspend fun onDestructiveMigration(connection: SQLiteConnection) {
        super.onDestructiveMigration(connection)
        databaseSchemeManager.clearSyncState()
    }
}
