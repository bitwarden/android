package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class DatabaseSchemeManagerTest {

    private val mockSettingsDiskSource: SettingsDiskSource = mockk {
        every { lastDatabaseSchemeChangeInstant } returns null
        every { lastDatabaseSchemeChangeInstant = any() } just runs
    }
    private val databaseSchemeManager = DatabaseSchemeManagerImpl(
        settingsDiskSource = mockSettingsDiskSource,
    )

    @Suppress("MaxLineLength")
    @Test
    fun `setLastDatabaseSchemeChangeInstant persists value in settingsDiskSource`() {
        databaseSchemeManager.lastDatabaseSchemeChangeInstant = FIXED_CLOCK.instant()
        verify {
            mockSettingsDiskSource.lastDatabaseSchemeChangeInstant = FIXED_CLOCK.instant()
        }
    }

    @Test
    fun `getLastDatabaseSchemeChangeInstant retrieves stored value from settingsDiskSource`() {
        databaseSchemeManager.lastDatabaseSchemeChangeInstant
        verify {
            mockSettingsDiskSource.lastDatabaseSchemeChangeInstant
        }
    }
}

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2023-10-27T12:00:00Z"),
    ZoneOffset.UTC,
)
