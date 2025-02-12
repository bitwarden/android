package com.bitwarden.authenticator.data.platform.manager

import com.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase

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
