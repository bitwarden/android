package com.x8bit.bitwarden.data.platform.manager

import android.util.Log
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.x8bit.bitwarden.BuildConfig
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.platform.datasource.disk.legacy.LegacyAppCenterMigrator
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.vault.manager.FileManager
import com.x8bit.bitwarden.ui.platform.util.toFormattedPattern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Clock

/**
 * [LogsManager] implementation for standard flavor builds.
 */
@OmitFromCoverage
class LogsManagerImpl(
    private val settingsRepository: SettingsRepository,
    legacyAppCenterMigrator: LegacyAppCenterMigrator,
    private val fileManager: FileManager,
    private val clock: Clock,
    dispatcherManager: DispatcherManager,
) : LogsManager {

    private val ioScope = CoroutineScope(dispatcherManager.io)
    private val nonfatalErrorTree: NonfatalErrorTree = NonfatalErrorTree()
    private val passkeyTree: PasskeyTree = PasskeyTree()

    override var isEnabled: Boolean
        get() = settingsRepository.isCrashLoggingEnabled
        set(value) {
            settingsRepository.isCrashLoggingEnabled = value
            Firebase.crashlytics.isCrashlyticsCollectionEnabled = value
            if (value) {
                Timber.plant(nonfatalErrorTree)
                Timber.plant(passkeyTree)
            } else {
                if (Timber.forest().contains(nonfatalErrorTree)) {
                    Timber.uproot(nonfatalErrorTree)
                }
                if (Timber.forest().contains(passkeyTree)) {
                    Timber.uproot(passkeyTree)
                }
            }
        }

    override fun setUserData(userId: String?, environmentType: Environment.Type) {
        Firebase.crashlytics.setUserId(userId.orEmpty())
        Firebase.crashlytics.setCustomKey(
            if (userId == null) "PreAuthRegion" else "Region",
            environmentType.toString(),
        )
    }

    override fun trackNonFatalException(throwable: Throwable) {
        if (isEnabled) {
            Firebase.crashlytics.recordException(throwable)
        }
    }

    override suspend fun getPasskeyLogs(): Result<String> = fileManager
        .readFromFile(LOG_DIR, LOG_FILE_NAME)

    init {
        legacyAppCenterMigrator.migrateIfNecessary()
        if (BuildConfig.HAS_LOGS_ENABLED) {
            Timber.plant(Timber.DebugTree())
        }
        isEnabled = settingsRepository.isCrashLoggingEnabled
    }

    private inner class NonfatalErrorTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            t?.let { trackNonFatalException(BitwardenNonfatalException(message, it)) }
        }
    }

    private inner class PasskeyTree : Timber.Tree() {
        /**
         * Write log to file, formatted as:
         * `<timestamp> <tag>    <priority> <message>`
         */
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (tag != "PASSKEY") return

            ioScope.launch {
                fileManager.writeToFile(
                    parent = LOG_DIR,
                    fileName = LOG_FILE_NAME,
                    data = "[${
                        clock
                            .instant()
                            .toFormattedPattern(LOG_TIMESTAMP_PATTERN, clock)
                    }] $tag\t${priority.toPriorityStringOrDefault("WTF")}  $message\n"
                )
            }
        }
    }
}

private class BitwardenNonfatalException(
    message: String,
    throwable: Throwable,
) : Exception(message, throwable)

private fun Int.toPriorityStringOrDefault(default: String = "-"): String = when (this) {
    Log.VERBOSE -> "V"
    Log.DEBUG -> "D"
    Log.INFO -> "I"
    Log.WARN -> "W"
    Log.ERROR -> "E"
    Log.ASSERT -> "A"
    else -> default
}

private const val LOG_FILE_NAME = "bitwarden.log"
private const val LOG_DIR = "logs/"
private const val LOG_TIMESTAMP_PATTERN = "yyyy-MM-dd HH:mm:ss"
