package com.x8bit.bitwarden.data.vault.datasource.disk.callback

import androidx.sqlite.SQLiteConnection
import com.x8bit.bitwarden.data.platform.manager.DatabaseSchemeManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Test

class DatabaseSchemeCallbackTest {

    private val databaseSchemeManager: DatabaseSchemeManager = mockk {
        every { clearSyncState() } just runs
    }
    private val callback = DatabaseSchemeCallback(databaseSchemeManager)

    @Test
    fun `onDestructiveMigration calls clearSyncState`() {
        callback.onDestructiveMigration(mockk<SQLiteConnection>())
        verify(exactly = 1) { databaseSchemeManager.clearSyncState() }
    }
}
