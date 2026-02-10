package com.x8bit.bitwarden.data.platform.manager.sdk.platformapi

import com.bitwarden.servercommunicationconfig.AcquiredCookie
import com.bitwarden.servercommunicationconfig.ServerCommunicationConfigPlatformApi
import com.x8bit.bitwarden.data.platform.error.CookiesRequiredException
import com.x8bit.bitwarden.data.platform.manager.CookieAcquisitionRequestManager
import com.x8bit.bitwarden.data.platform.manager.model.CookieAcquisitionRequest

/**
 * Implementation of SDK's [ServerCommunicationConfigPlatformApi].
 *
 * When cookies are required, signals via [CookieAcquisitionRequestManager] StateFlow for
 * navigation, then throws [CookiesRequiredException] to cancel the API request.
 *
 * @property serverCommConfigManager Manager that exposes pending cookie state for navigation.
 */
class ServerCommunicationConfigPlatformApiImpl(
    private val serverCommConfigManager: CookieAcquisitionRequestManager,
) : ServerCommunicationConfigPlatformApi {

    override suspend fun acquireCookies(hostname: String): List<AcquiredCookie>? {
        // Signal via internal state pattern (Manager StateFlow)
        serverCommConfigManager.setPendingCookieAcquisition(
            CookieAcquisitionRequest(
                hostname = hostname,
            ),
        )

        // Throw exception to cancel the current API request. The exception terminates the SDK
        // call. The Manager StateFlow emission above triggers navigation to the cookie acquisition
        // screen via RootNavViewModel. User will manually retry the original operation after
        // acquiring cookies.
        throw CookiesRequiredException(hostname)
    }
}
