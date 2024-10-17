package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.platform.datasource.disk.legacy.LegacyAppCenterMigrator
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository

/**
 * CrashLogsManager implementation for F-droid flavor builds.
 */
class CrashLogsManagerImpl(
    settingsRepository: SettingsRepository,
    legacyAppCenterMigrator: LegacyAppCenterMigrator,
) : CrashLogsManager {
    override var isEnabled: Boolean = true

    override fun trackNonFatalException(throwable: Throwable) = Unit
}
