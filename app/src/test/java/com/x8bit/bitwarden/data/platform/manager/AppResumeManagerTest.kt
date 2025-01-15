package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.util.FakeSettingsDiskSource
import com.x8bit.bitwarden.data.platform.datasource.network.di.PlatformNetworkModule
import com.x8bit.bitwarden.data.platform.manager.model.AppResumeScreenData
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.vault.manager.VaultLockManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class AppResumeManagerTest {

    private val authDiskSource: AuthDiskSource = mockk {
        every { getLastLockTimestamp(any()) } returns Instant.now().toEpochMilli()
    }
    private val fakeSettingsDiskSource: SettingsDiskSource = FakeSettingsDiskSource()
    private val authRepository = mockk<AuthRepository> {
        every { activeUserId } returns USER_ID
    }
    private val vaultLockManager: VaultLockManager = mockk {
        every { isVaultUnlocked(USER_ID) } returns true
    }

    private val appResumeManager = AppResumeManagerImpl(
        settingsDiskSource = fakeSettingsDiskSource,
        authDiskSource = authDiskSource,
        authRepository = authRepository,
        vaultLockManager = vaultLockManager,

    )

    private val json = PlatformNetworkModule.providesJson()

    @Suppress("MaxLineLength")
    @Test
    fun `setResumeScreen should update the app resume screen in the settings disk source`() =
        runTest {
            val expectedValue = AppResumeScreenData.SendScreen
            appResumeManager.setResumeScreen(expectedValue)
            val actualValue = fakeSettingsDiskSource.getAppResumeScreen(USER_ID)

            assertEquals(expectedValue, actualValue)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getResumeScreen should return null when there is no app resume screen saved`() =
        runTest {
            val actualValue = appResumeManager.getResumeScreen()
            assertNull(actualValue)
        }

    @Suppress("MaxLineLength")
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

    @Suppress("MaxLineLength")
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
            val expectedValue = SpecialCircumstance.SearchShortcut("test")
            val actualValue = appResumeManager.getResumeSpecialCircumstance()
            assertEquals(expectedValue, actualValue)
        }
}

private const val USER_ID = "user_id"
