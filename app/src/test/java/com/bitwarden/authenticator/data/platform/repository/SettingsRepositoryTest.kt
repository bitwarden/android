package com.bitwarden.authenticator.data.platform.repository

import com.bitwarden.authenticator.data.auth.datasource.disk.AuthDiskSource
import com.bitwarden.authenticator.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.bitwarden.authenticator.data.authenticator.datasource.sdk.AuthenticatorSdkSource
import com.bitwarden.authenticator.data.platform.base.FakeDispatcherManager
import com.bitwarden.authenticator.data.platform.datasource.disk.SettingsDiskSource
import com.bitwarden.authenticator.data.platform.manager.BiometricsEncryptionManager
import com.bitwarden.authenticator.ui.platform.feature.settings.data.model.DefaultSaveOption
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SettingsRepositoryTest {

    private val settingsDiskSource: SettingsDiskSource = mockk {
        every { getAlertThresholdSeconds() } returns 7
    }
    private val authDiskSource: AuthDiskSource = FakeAuthDiskSource()
    private val biometricsEncryptionManager: BiometricsEncryptionManager = mockk()
    private val authenticatorSdkSource: AuthenticatorSdkSource = mockk()

    private val settingsRepository = SettingsRepositoryImpl(
        settingsDiskSource = settingsDiskSource,
        authDiskSource = authDiskSource,
        biometricsEncryptionManager = biometricsEncryptionManager,
        authenticatorSdkSource = authenticatorSdkSource,
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
}
