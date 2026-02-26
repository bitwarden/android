package com.bitwarden.authenticator.data.platform.repository

import app.cash.turbine.test
import com.bitwarden.authenticator.data.platform.datasource.disk.SettingsDiskSource
import com.bitwarden.authenticator.ui.platform.feature.settings.data.model.DefaultSaveOption
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SettingsRepositoryTest {

    private val settingsDiskSource: SettingsDiskSource = mockk {
        every { getAlertThresholdSeconds() } returns 7
    }

    private val settingsRepository: SettingsRepository = SettingsRepositoryImpl(
        settingsDiskSource = settingsDiskSource,
        flightRecorderManager = mockk(),
        dispatcherManager = FakeDispatcherManager(),
    )

    @Test
    fun `hasUserDismissedDownloadBitwardenCard should return false when disk source is null`() {
        every { settingsDiskSource.hasUserDismissedDownloadBitwardenCard } returns null
        assertFalse(settingsRepository.hasUserDismissedDownloadBitwardenCard)
    }

    @Test
    fun `hasUserDismissedDownloadBitwardenCard should return false when disk source is false`() {
        every { settingsDiskSource.hasUserDismissedDownloadBitwardenCard } returns false
        assertFalse(settingsRepository.hasUserDismissedDownloadBitwardenCard)
    }

    @Test
    fun `hasUserDismissedDownloadBitwardenCard should return true when disk source is true`() {
        every { settingsDiskSource.hasUserDismissedDownloadBitwardenCard } returns true
        assertTrue(settingsRepository.hasUserDismissedDownloadBitwardenCard)
    }

    @Test
    fun `hasUserDismissedSyncWithBitwardenCard should return false when disk source is null`() {
        every { settingsDiskSource.hasUserDismissedSyncWithBitwardenCard } returns null
        assertFalse(settingsRepository.hasUserDismissedSyncWithBitwardenCard)
    }

    @Test
    fun `hasUserDismissedSyncWithBitwardenCard should return false when disk source is false`() {
        every { settingsDiskSource.hasUserDismissedSyncWithBitwardenCard } returns false
        assertFalse(settingsRepository.hasUserDismissedSyncWithBitwardenCard)
    }

    @Test
    fun `hasUserDismissedSyncWithBitwardenCard should return true when disk source is true`() {
        every { settingsDiskSource.hasUserDismissedSyncWithBitwardenCard } returns true
        assertTrue(settingsRepository.hasUserDismissedSyncWithBitwardenCard)
    }

    @Test
    fun `hasUserDismissedSyncWithBitwardenCard set should set disk source`() {
        every { settingsDiskSource.hasUserDismissedSyncWithBitwardenCard = true } just runs
        settingsRepository.hasUserDismissedSyncWithBitwardenCard = true
        verify { settingsRepository.hasUserDismissedSyncWithBitwardenCard = true }
    }

    @Test
    fun `defaultSaveOption should pull from and update SettingsDiskSource`() {
        // Reading from repository should read from disk source:
        every { settingsDiskSource.defaultSaveOption } returns DefaultSaveOption.NONE
        assertEquals(
            DefaultSaveOption.NONE,
            settingsRepository.defaultSaveOption,
        )
        verify { settingsDiskSource.defaultSaveOption }

        // Writing to repository should write to disk source:
        every { settingsDiskSource.defaultSaveOption = DefaultSaveOption.BITWARDEN_APP } just runs
        settingsRepository.defaultSaveOption = DefaultSaveOption.BITWARDEN_APP
        verify { settingsDiskSource.defaultSaveOption = DefaultSaveOption.BITWARDEN_APP }
    }

    @Test
    fun `defaultSaveOptionFlow should match SettingsDiskSource`() = runTest {
        // Reading from repository should read from disk source:
        val expectedOptions = listOf(
            DefaultSaveOption.NONE,
            DefaultSaveOption.LOCAL,
            DefaultSaveOption.BITWARDEN_APP,
            DefaultSaveOption.NONE,
        )
        every { settingsDiskSource.defaultSaveOptionFlow } returns flow {
            expectedOptions.forEach { emit(it) }
        }

        settingsRepository.defaultSaveOptionFlow.test {
            expectedOptions.forEach {
                assertEquals(it, awaitItem())
            }
            awaitComplete()
        }
    }

    @Test
    fun `isDynamicColorsEnabled should pull from and update SettingsDiskSource`() {
        // Reading from repository should read from disk source:
        every { settingsDiskSource.isDynamicColorsEnabled } returns null
        assertFalse(settingsRepository.isDynamicColorsEnabled)
        verify { settingsDiskSource.isDynamicColorsEnabled }

        // Writing to repository should write to disk source:
        every { settingsDiskSource.isDynamicColorsEnabled = true } just runs
        settingsRepository.isDynamicColorsEnabled = true
        verify { settingsDiskSource.isDynamicColorsEnabled = true }
    }

    @Test
    fun `isDynamicColorsEnabledFlow should match SettingsDiskSource`() = runTest {
        // Reading from repository should read from disk source:
        val mutableDynamicColorsFlow = bufferedMutableSharedFlow<Boolean?>()
        every { settingsDiskSource.isDynamicColorsEnabledFlow } returns mutableDynamicColorsFlow
        every { settingsDiskSource.isDynamicColorsEnabled } returns null

        settingsRepository.isDynamicColorsEnabledFlow.test {
            assertFalse(awaitItem())
            mutableDynamicColorsFlow.emit(true)
            assertTrue(awaitItem())
            mutableDynamicColorsFlow.emit(false)
            assertFalse(awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `previouslySyncedBitwardenAccountIds should pull from and update SettingsDiskSource`() {
        // Reading from repository should read from disk source:
        every { settingsDiskSource.previouslySyncedBitwardenAccountIds } returns emptySet()
        assertEquals(
            emptySet<String>(),
            settingsRepository.previouslySyncedBitwardenAccountIds,
        )
        verify { settingsDiskSource.previouslySyncedBitwardenAccountIds }

        // Writing to repository should write to disk source:
        every {
            settingsDiskSource.previouslySyncedBitwardenAccountIds = setOf("1", "2", "3")
        } just runs
        settingsRepository.previouslySyncedBitwardenAccountIds = setOf("1", "2", "3")
        verify { settingsDiskSource.previouslySyncedBitwardenAccountIds = setOf("1", "2", "3") }
    }
}
