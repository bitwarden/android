package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.util.FakeSettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.model.AppResumeScreenData
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.vault.manager.VaultLockManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class AppResumeManagerTest {
    private val fakeSettingsDiskSource: SettingsDiskSource = FakeSettingsDiskSource()
    private val authRepository = mockk<AuthRepository> {
        every { activeUserId } returns USER_ID
    }
    private val vaultLockManager: VaultLockManager = mockk {
        every { isVaultUnlocked(USER_ID) } returns true
    }

    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )

    private val fakeAuthDiskSource = FakeAuthDiskSource()

    private val appResumeManager = AppResumeManagerImpl(
        settingsDiskSource = fakeSettingsDiskSource,
        authDiskSource = fakeAuthDiskSource,
        authRepository = authRepository,
        vaultLockManager = vaultLockManager,
        clock = fixedClock,
    )

    @Test
    fun `setResumeScreen should update the app resume screen in the settings disk source`() =
        runTest {
            val expectedValue = AppResumeScreenData.SendScreen
            appResumeManager.setResumeScreen(expectedValue)
            val actualValue = fakeSettingsDiskSource.getAppResumeScreen(USER_ID)

            assertEquals(expectedValue, actualValue)
        }

    @Test
    fun `getResumeScreen should return null when there is no app resume screen saved`() =
        runTest {
            val actualValue = appResumeManager.getResumeScreen()
            assertNull(actualValue)
        }

    @Test
    fun `getResumeScreen should return the saved AppResumeScreen`() =
        runTest {
            val expectedValue = AppResumeScreenData.GeneratorScreen
            fakeSettingsDiskSource.storeAppResumeScreen(
                userId = USER_ID,
                screenData = expectedValue,
            )
            val actualValue = appResumeManager.getResumeScreen()
            assertEquals(expectedValue, actualValue)
        }

    @Test
    fun `clearResumeScreen should clear the app resume screen in the settings disk source`() =
        runTest {
            fakeSettingsDiskSource.storeAppResumeScreen(
                userId = USER_ID,
                screenData = AppResumeScreenData.GeneratorScreen,
            )
            appResumeManager.clearResumeScreen()
            val actualValue = fakeSettingsDiskSource.getAppResumeScreen(USER_ID)
            assertNull(actualValue)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getResumeSpecialCircumstance should return GeneratorShortcut when the resume screen is GeneratorScreen`() =
        runTest {
            fakeSettingsDiskSource.storeAppResumeScreen(
                userId = USER_ID,
                screenData = AppResumeScreenData.GeneratorScreen,
            )
            fakeAuthDiskSource.storeLastLockTimestamp(USER_ID, fixedClock.instant())
            val expectedValue = SpecialCircumstance.GeneratorShortcut
            val actualValue = appResumeManager.getResumeSpecialCircumstance()
            assertEquals(expectedValue, actualValue)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getResumeSpecialCircumstance should return SendShortcut when the resume screen is SendScreen`() =
        runTest {
            fakeSettingsDiskSource.storeAppResumeScreen(
                userId = USER_ID,
                screenData = AppResumeScreenData.SendScreen,
            )
            fakeAuthDiskSource.storeLastLockTimestamp(USER_ID, fixedClock.instant())
            val expectedValue = SpecialCircumstance.SendShortcut
            val actualValue = appResumeManager.getResumeSpecialCircumstance()
            assertEquals(expectedValue, actualValue)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getResumeSpecialCircumstance should return VerificationCodeShortcut when the resume screen is VerificationCodeScreen`() =
        runTest {
            fakeSettingsDiskSource.storeAppResumeScreen(
                userId = USER_ID,
                screenData = AppResumeScreenData.VerificationCodeScreen,
            )
            fakeAuthDiskSource.storeLastLockTimestamp(USER_ID, fixedClock.instant())
            val expectedValue = SpecialCircumstance.VerificationCodeShortcut
            val actualValue = appResumeManager.getResumeSpecialCircumstance()
            assertEquals(expectedValue, actualValue)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getResumeSpecialCircumstance should return SearchShortcut when the resume screen is SearchScreen`() =
        runTest {
            fakeSettingsDiskSource.storeAppResumeScreen(
                userId = USER_ID,
                screenData = AppResumeScreenData.SearchScreen("test"),
            )
            fakeAuthDiskSource.storeLastLockTimestamp(USER_ID, fixedClock.instant())
            val expectedValue = SpecialCircumstance.SearchShortcut("test")
            val actualValue = appResumeManager.getResumeSpecialCircumstance()
            assertEquals(expectedValue, actualValue)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getResumeSpecialCircumstance should should clear app resume screen if have passed 5 minutes`() {
        val delayedAuthDiskSource: AuthDiskSource = mockk {
            every { getLastLockTimestamp(any()) } returns fixedClock.instant()
                .minusSeconds(5 * 60 + 1)
        }

        val delayedAppResumeManager = AppResumeManagerImpl(
            settingsDiskSource = fakeSettingsDiskSource,
            authDiskSource = delayedAuthDiskSource,
            authRepository = authRepository,
            vaultLockManager = vaultLockManager,
            clock = fixedClock,
        )
        fakeSettingsDiskSource.storeAppResumeScreen(
            userId = USER_ID,
            screenData = AppResumeScreenData.GeneratorScreen,
        )
        val actualValue = delayedAppResumeManager.getResumeSpecialCircumstance()
        assertNull(actualValue)

        val actualSettingsValue = fakeSettingsDiskSource.getAppResumeScreen(
            userId = USER_ID,
        )
        assertNull(actualSettingsValue)
    }
}

private const val USER_ID = "user_id"
