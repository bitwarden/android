package com.x8bit.bitwarden.data.vault.datasource.disk.callback

import com.x8bit.bitwarden.data.platform.manager.DatabaseSchemeManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Test
import java.time.Clock
import java.time.Instant

class DatabaseSchemeCallbackTest {

    private val databaseSchemeManager: DatabaseSchemeManager = mockk()
    private val clock: Clock = mockk()
    private val callback = DatabaseSchemeCallback(databaseSchemeManager, clock)

    @Test
    fun `onDestructiveMigration updates lastDatabaseSchemeChangeInstant`() {
        val now = Instant.now()
        every { clock.instant() } returns now
        every { databaseSchemeManager.lastDatabaseSchemeChangeInstant = any() } just runs

        callback.onDestructiveMigration(mockk())

        verify { databaseSchemeManager.lastDatabaseSchemeChangeInstant = now }
    }
}
