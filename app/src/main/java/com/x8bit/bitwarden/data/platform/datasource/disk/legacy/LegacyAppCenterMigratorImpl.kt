package com.x8bit.bitwarden.data.platform.datasource.disk.legacy

import android.content.SharedPreferences
import androidx.core.content.edit
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository

private const val LEGACY_ENABLED_CRASHES = "enabled_Crashes"

/**
 * Primary implementation of [LegacyAppCenterMigrator].
 */
class LegacyAppCenterMigratorImpl(
    private val settingsRepository: SettingsRepository,
    private val appCenterPreferences: SharedPreferences,
) : LegacyAppCenterMigrator {
    override fun migrateIfNecessary() {
        // If the data is not present, then we return since there is nothing to migrate.
        if (!appCenterPreferences.contains(LEGACY_ENABLED_CRASHES)) return
        settingsRepository.isCrashLoggingEnabled = appCenterPreferences.getBoolean(
            LEGACY_ENABLED_CRASHES,
            true,
        )
        appCenterPreferences.edit { clear() }
    }
}
