package com.x8bit.bitwarden.data.platform.datasource.disk.legacy

import androidx.core.content.edit
import com.x8bit.bitwarden.data.platform.base.FakeSharedPreferences
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class LegacyAppCenterMigratorTest {
    private val settingsRepository = mockk<SettingsRepository>()
    private val fakeSharedPreferences = FakeSharedPreferences()

    private val migrator = LegacyAppCenterMigratorImpl(
        settingsRepository = settingsRepository,
        appCenterPreferences = fakeSharedPreferences,
    )

    @Test
    fun `migrateIfNecessary should do nothing when there is nothing to migrate`() {
        migrator.migrateIfNecessary()

        verify(exactly = 0) {
            settingsRepository.isCrashLoggingEnabled = any()
        }
    }

    @Test
    fun `migrateIfNecessary should migrate the data when it is present and true`() {
        val isEnabled = true
        fakeSharedPreferences.edit { putBoolean("enabled_Crashes", isEnabled) }
        every { settingsRepository.isCrashLoggingEnabled = isEnabled } just runs

        migrator.migrateIfNecessary()

        verify(exactly = 1) {
            settingsRepository.isCrashLoggingEnabled = isEnabled
        }
        assertFalse(fakeSharedPreferences.contains("enabled_Crashes"))
    }

    @Test
    fun `migrateIfNecessary should migrate the data when it is present and false`() {
        val isEnabled = false
        fakeSharedPreferences.edit { putBoolean("enabled_Crashes", isEnabled) }
        every { settingsRepository.isCrashLoggingEnabled = isEnabled } just runs

        migrator.migrateIfNecessary()

        verify(exactly = 1) {
            settingsRepository.isCrashLoggingEnabled = isEnabled
        }
        assertFalse(fakeSharedPreferences.contains("enabled_Crashes"))
    }
}
