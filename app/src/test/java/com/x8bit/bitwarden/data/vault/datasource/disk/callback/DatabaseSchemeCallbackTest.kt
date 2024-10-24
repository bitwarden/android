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
import java.time.ZoneOffset

class DatabaseSchemeCallbackTest {

    private val databaseSchemeManager: DatabaseSchemeManager = mockk {
        every { lastDatabaseSchemeChangeInstant = any() } just runs
    }
    private val callback = DatabaseSchemeCallback(databaseSchemeManager, FIXED_CLOCK)

    @Test
    fun `onDestructiveMigration updates lastDatabaseSchemeChangeInstant`() {
        callback.onDestructiveMigration(mockk())

        verify { databaseSchemeManager.lastDatabaseSchemeChangeInstant = FIXED_CLOCK.instant() }
    }
}

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2023-10-27T12:00:00Z"),
    ZoneOffset.UTC,
)
