package com.x8bit.bitwarden

import android.app.Application
import com.bitwarden.core.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.auth.manager.AuthRequestNotificationManager
import com.x8bit.bitwarden.data.platform.manager.LogsManager
import com.x8bit.bitwarden.data.platform.manager.event.OrganizationEventManager
import com.x8bit.bitwarden.data.platform.manager.network.NetworkConfigManager
import com.x8bit.bitwarden.data.platform.manager.network.NetworkConnectionManager
import com.x8bit.bitwarden.data.platform.manager.restriction.RestrictionManager
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
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
    lateinit var logsManager: LogsManager

    @Inject
    lateinit var networkConnectionManager: NetworkConnectionManager

    @Inject
    lateinit var networkConfigManager: NetworkConfigManager

    @Inject
    lateinit var authRequestNotificationManager: AuthRequestNotificationManager

    @Inject
    lateinit var organizationEventManager: OrganizationEventManager

    @Inject
    lateinit var restrictionManager: RestrictionManager

    override fun onLowMemory() {
        super.onLowMemory()
        Timber.w("onLowMemory")
    }
}
