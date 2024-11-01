package com.x8bit.bitwarden.data.vault.datasource.disk.callback

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.x8bit.bitwarden.data.platform.manager.DatabaseSchemeManager
import java.time.Clock

/**
 * A [RoomDatabase.Callback] for tracking database scheme changes.
 */
class DatabaseSchemeCallback(
    private val databaseSchemeManager: DatabaseSchemeManager,
    private val clock: Clock,
) : RoomDatabase.Callback() {
    override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
        databaseSchemeManager.lastDatabaseSchemeChangeInstant = clock.instant()
    }
}
