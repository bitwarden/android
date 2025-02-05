package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.BuildConfig
import com.x8bit.bitwarden.data.platform.datasource.disk.legacy.LegacyAppCenterMigrator
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.vault.manager.FileManager
import timber.log.Timber
import java.time.Clock

/**
 * [LogsManager] implementation for F-droid flavor builds.
 */
class LogsManagerImpl(
    settingsRepository: SettingsRepository,
    legacyAppCenterMigrator: LegacyAppCenterMigrator,
    fileManager: FileManager,
    clock: Clock,
    dispatcherManager: DispatcherManager,
) : LogsManager {
    init {
        if (BuildConfig.HAS_LOGS_ENABLED) {
            Timber.plant(Timber.DebugTree())
        }
    }

    override var isEnabled: Boolean = false

    override fun setUserData(userId: String?, environmentType: Environment.Type) = Unit

    override fun trackNonFatalException(throwable: Throwable) = Unit

    override suspend fun getPasskeyLogs(): Result<String> = Result.failure(NotImplementedError())
}
