package com.x8bit.bitwarden.data.platform.manager

import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.platform.datasource.disk.legacy.LegacyAppCenterMigrator
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository

/**
 * CrashLogsManager implementation for standard flavor builds.
 */
@OmitFromCoverage
class CrashLogsManagerImpl(
    private val settingsRepository: SettingsRepository,
    legacyAppCenterMigrator: LegacyAppCenterMigrator,
) : CrashLogsManager {

    override var isEnabled: Boolean
        get() = settingsRepository.isCrashLoggingEnabled
        set(value) {
            settingsRepository.isCrashLoggingEnabled = value
            Firebase.crashlytics.isCrashlyticsCollectionEnabled = value
        }

    override fun trackNonFatalException(throwable: Throwable) {
        if (settingsRepository.isCrashLoggingEnabled) {
            Firebase.crashlytics.recordException(throwable)
        }
    }

    init {
        legacyAppCenterMigrator.migrateIfNecessary()
        isEnabled = settingsRepository.isCrashLoggingEnabled
    }
}
