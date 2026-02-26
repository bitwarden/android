package com.x8bit.bitwarden.data.platform.manager.network

import com.bitwarden.data.datasource.disk.ConfigDiskSource
import com.bitwarden.network.model.NetworkCookie
import com.x8bit.bitwarden.data.platform.datasource.disk.CookieDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.model.CookieConfigurationData
import com.x8bit.bitwarden.data.platform.manager.CookieAcquisitionRequestManager
import com.x8bit.bitwarden.data.platform.manager.model.CookieAcquisitionRequest
import com.x8bit.bitwarden.data.platform.manager.util.toNetworkCookieList

private const val BOOTSTRAP_TYPE_SSO_COOKIE_VENDOR = "ssoCookieVendor"

/**
 * Default implementation of [NetworkCookieManager].
 */
class NetworkCookieManagerImpl(
    private val configDiskSource: ConfigDiskSource,
    private val cookieDiskSource: CookieDiskSource,
    private val cookieAcquisitionRequestManager: CookieAcquisitionRequestManager,
) : NetworkCookieManager {

    override fun needsBootstrap(hostname: String): Boolean = configDiskSource
        .serverConfig
        ?.serverData
        ?.communication
        ?.bootstrap
        ?.type
        ?.let { bootstrapType ->
            when (bootstrapType) {
                BOOTSTRAP_TYPE_SSO_COOKIE_VENDOR -> {
                    // When the bootstrap type is SSO cookie vendor, but we do not yet have any
                    // cookies, the cookie manager needs to be bootstrapped. This includes the
                    // case where no cookie config exists for the hostname at all.
                    cookieDiskSource
                        .getCookieConfig(hostname = hostname)
                        ?.cookies
                        ?.none() != false
                }

                else -> false
            }
        }
        ?: false

    override fun getCookies(hostname: String): List<NetworkCookie> = cookieDiskSource
        .getCookieConfig(hostname = hostname)
        ?.cookies
        .toNetworkCookieList()

    override fun acquireCookies(hostname: String) {
        cookieAcquisitionRequestManager.setPendingCookieAcquisition(
            CookieAcquisitionRequest(
                hostname = hostname,
            ),
        )
    }

    override fun storeCookies(hostname: String, cookies: Map<String, String>) {
        cookieDiskSource.storeCookieConfig(
            hostname = hostname,
            config = CookieConfigurationData(
                hostname = hostname,
                cookies = cookies.map { (name, value) ->
                    CookieConfigurationData.Cookie(name = name, value = value)
                },
            ),
        )
    }
}
