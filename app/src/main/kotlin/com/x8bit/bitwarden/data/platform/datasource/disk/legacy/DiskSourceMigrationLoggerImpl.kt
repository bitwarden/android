package com.x8bit.bitwarden.data.platform.datasource.disk.legacy

import android.content.SharedPreferences
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.data.manager.flightrecorder.FlightRecorderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

/**
 * The default implementation of the [DiskSourceMigrationLogger].
 */
@OmitFromCoverage
internal class DiskSourceMigrationLoggerImpl(
    keystoreEncryptedPreferences: SharedPreferences,
    encryptedPreferences: SharedPreferences,
    flightRecorderManager: FlightRecorderManager,
    dispatcherManager: DispatcherManager,
) : DiskSourceMigrationLogger {
    private val unconfinedScope: CoroutineScope = CoroutineScope(dispatcherManager.unconfined)

    init {
        flightRecorderManager
            .isLoggingReadyFlow
            .filter { isLoggingReady ->
                isLoggingReady &&
                    keystoreEncryptedPreferences.all.isNotEmpty() &&
                    encryptedPreferences.all.isEmpty()
            }
            .onEach { Timber.d("Encrypted data has been migrated to Keystore encryption.") }
            .launchIn(unconfinedScope)
    }
}
