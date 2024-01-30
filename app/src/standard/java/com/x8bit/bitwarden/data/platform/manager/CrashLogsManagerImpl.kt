package com.x8bit.bitwarden.data.platform.manager

import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository

/**
 * CrashLogsManager implementation for standard flavor builds.
 */
class CrashLogsManagerImpl(
    private val settingsRepository: SettingsRepository,
) : CrashLogsManager {

    override var isEnabled: Boolean
        get() = settingsRepository.isCrashLoggingEnabled
        set(value) {
            settingsRepository.isCrashLoggingEnabled = value
            Firebase.crashlytics.setCrashlyticsCollectionEnabled(value)
        }

    init {
        isEnabled = settingsRepository.isCrashLoggingEnabled
    }
}
