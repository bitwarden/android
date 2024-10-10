package com.bitwarden.authenticator.data.platform.repository

import com.bitwarden.authenticator.data.auth.datasource.disk.AuthDiskSource
import com.bitwarden.authenticator.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.bitwarden.authenticator.data.authenticator.datasource.sdk.AuthenticatorSdkSource
import com.bitwarden.authenticator.data.platform.base.FakeDispatcherManager
import com.bitwarden.authenticator.data.platform.datasource.disk.SettingsDiskSource
import com.bitwarden.authenticator.data.platform.manager.BiometricsEncryptionManager
import io.mockk.every
import io.mockk.mockk
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
}
