package com.x8bit.bitwarden.data.platform.manager.sdk.platformapi

import com.bitwarden.servercommunicationconfig.AcquiredCookie
import com.bitwarden.servercommunicationconfig.ServerCommunicationConfigPlatformApi
import com.x8bit.bitwarden.data.platform.error.CookiesRequiredException
import com.x8bit.bitwarden.data.platform.manager.CookieAcquisitionRequestManager
import com.x8bit.bitwarden.data.platform.manager.model.CookieAcquisitionRequest

/**
 * Implementation of SDK's [ServerCommunicationConfigPlatformApi].
 *
 * This is an SDK callback interface required by the SDK contract. The SDK's intended design is for
 * [acquireCookies] to block while fetching cookies from the server cookie vending endpoint, return
 * them, and have the SDK automatically retry the failed request. However, cookie acquisition
 * requires async user interaction (browser authentication + deep link callback), which cannot be
 * performed within a blocking suspend call.
 *
 * Because of this constraint, cookie acquisition is currently handled entirely outside the SDK
 * context: our interceptor detects 302 redirects, the UI prompts the user, cookies are obtained
 * via browser and stored directly. This implementation exists as a defensive fallback — if the SDK
 * ever invokes [acquireCookies] internally, it:
 * 1. Emits a [CookieAcquisitionRequest] via [CookieAcquisitionRequestManager] StateFlow to
 *    signal the UI to navigate to the cookie acquisition screen.
 * 2. Throws [CookiesRequiredException] to abort the SDK's current call chain.
 *
 * Note: Future SDK versions may expose atomic cookie setters, removing the need for this blocking
 * acquisition pattern entirely.
 *
 * @property serverCommConfigManager Manager that exposes pending cookie acquisition state for
 * navigation.
 */
class ServerCommunicationConfigPlatformApiImpl(
    private val serverCommConfigManager: CookieAcquisitionRequestManager,
) : ServerCommunicationConfigPlatformApi {

    /**
     * SDK callback for cookie acquisition. Not invoked during normal app operation — cookie
     * acquisition is handled outside the SDK context. This serves as a defensive implementation
     * that signals the UI and aborts the SDK operation if called unexpectedly.
     *
     * This method never returns normally.
     *
     * @throws CookiesRequiredException Always thrown to abort the SDK call chain.
     */
    override suspend fun acquireCookies(hostname: String): List<AcquiredCookie>? {
        serverCommConfigManager.setPendingCookieAcquisition(
            CookieAcquisitionRequest(
                hostname = hostname,
            ),
        )
        throw CookiesRequiredException(hostname)
    }
}
