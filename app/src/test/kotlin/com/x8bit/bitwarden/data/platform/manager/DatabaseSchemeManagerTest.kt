package com.x8bit.bitwarden.data.platform.manager

import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.util.FakeSettingsDiskSource
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class DatabaseSchemeManagerTest {

    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val fakeSettingsDiskSource = FakeSettingsDiskSource()
    private val databaseSchemeManager = DatabaseSchemeManagerImpl(
        authDiskSource = fakeAuthDiskSource,
        settingsDiskSource = fakeSettingsDiskSource,
    )

    @BeforeEach
    fun setup() {
        fakeAuthDiskSource.userState = USER_STATE
        fakeSettingsDiskSource.storeLastSyncTime(USER_ID_1, FIXED_CLOCK.instant())
        fakeSettingsDiskSource.storeLastSyncTime(USER_ID_2, FIXED_CLOCK.instant())
    }

    @Test
    fun `clearSyncState clears lastSyncTimes and emit`() = runTest {
        assertNotNull(fakeSettingsDiskSource.getLastSyncTime(USER_ID_1))
        assertNotNull(fakeSettingsDiskSource.getLastSyncTime(USER_ID_2))

        databaseSchemeManager.databaseSchemeChangeFlow.test {
            databaseSchemeManager.clearSyncState()
            awaitItem()
            expectNoEvents()
        }

        assertNull(fakeSettingsDiskSource.getLastSyncTime(USER_ID_1))
        assertNull(fakeSettingsDiskSource.getLastSyncTime(USER_ID_2))
    }
}

private const val USER_ID_1: String = "USER_ID_1"
private const val USER_ID_2: String = "USER_ID_2"

private val USER_STATE: UserStateJson = UserStateJson(
    activeUserId = USER_ID_1,
    accounts = mapOf(
        USER_ID_1 to mockk(),
        USER_ID_2 to mockk(),
    ),
)

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2023-10-27T12:00:00Z"),
    ZoneOffset.UTC,
)
