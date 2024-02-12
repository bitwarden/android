package com.x8bit.bitwarden

import android.app.Application
import com.x8bit.bitwarden.data.auth.manager.AuthRequestNotificationManager
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.platform.manager.CrashLogsManager
import com.x8bit.bitwarden.data.platform.manager.NetworkConfigManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Custom application class.
 */
@OmitFromCoverage
@HiltAndroidApp
class BitwardenApplication : Application() {
    // Inject classes here that must be triggered on startup but are not otherwise consumed by
    // other callers.
    @Inject
    lateinit var networkConfigManager: NetworkConfigManager

    @Inject
    lateinit var crashLogsManager: CrashLogsManager

    @Inject
    lateinit var authRequestNotificationManager: AuthRequestNotificationManager
}
