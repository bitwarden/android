package com.x8bit.bitwarden

import android.app.Application
import com.x8bit.bitwarden.data.auth.manager.AuthRequestNotificationManager
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.platform.manager.LogsManager
import com.x8bit.bitwarden.data.platform.manager.NetworkConfigManager
import com.x8bit.bitwarden.data.platform.manager.event.OrganizationEventManager
import com.x8bit.bitwarden.data.platform.manager.restriction.RestrictionManager
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
    lateinit var logsManager: LogsManager

    @Inject
    lateinit var networkConfigManager: NetworkConfigManager

    @Inject
    lateinit var authRequestNotificationManager: AuthRequestNotificationManager

    @Inject
    lateinit var organizationEventManager: OrganizationEventManager

    @Inject
    lateinit var restrictionManager: RestrictionManager
}
