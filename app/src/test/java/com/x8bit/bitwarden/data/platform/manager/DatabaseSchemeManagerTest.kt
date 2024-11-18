package com.x8bit.bitwarden.data.platform.manager

import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class DatabaseSchemeManagerTest {

    private val mutableLastDatabaseSchemeChangeInstantFlow = MutableStateFlow<Instant?>(null)
    private val mockSettingsDiskSource: SettingsDiskSource = mockk {
        every {
            lastDatabaseSchemeChangeInstant
        } returns mutableLastDatabaseSchemeChangeInstantFlow.value
        every { lastDatabaseSchemeChangeInstant = any() } answers {
            mutableLastDatabaseSchemeChangeInstantFlow.value = firstArg()
        }
        every {
            lastDatabaseSchemeChangeInstantFlow
        } returns mutableLastDatabaseSchemeChangeInstantFlow
    }
    private val dispatcherManager = FakeDispatcherManager()
    private val databaseSchemeManager = DatabaseSchemeManagerImpl(
        settingsDiskSource = mockSettingsDiskSource,
        dispatcherManager = dispatcherManager,
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
    fun `setLastDatabaseSchemeChangeInstant does emit value`() = runTest {
        databaseSchemeManager.lastDatabaseSchemeChangeInstantFlow.test {
            // Assert the value is initialized to null
            assertEquals(
                null,
                awaitItem(),
            )
            // Assert the new value is emitted
            databaseSchemeManager.lastDatabaseSchemeChangeInstant = FIXED_CLOCK.instant()
            assertEquals(
                FIXED_CLOCK.instant(),
                awaitItem(),
            )
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
