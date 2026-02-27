package com.x8bit.bitwarden.data.platform.manager.network

import com.bitwarden.data.datasource.disk.ConfigDiskSource
import com.bitwarden.network.model.NetworkCookie
import com.x8bit.bitwarden.data.platform.datasource.disk.CookieDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.model.CookieConfigurationData
import com.x8bit.bitwarden.data.platform.manager.CookieAcquisitionRequestManager
import com.x8bit.bitwarden.data.platform.manager.model.CookieAcquisitionRequest
import com.x8bit.bitwarden.data.platform.manager.util.toNetworkCookieList
import timber.log.Timber

private const val BOOTSTRAP_TYPE_SSO_COOKIE_VENDOR = "ssoCookieVendor"

/**
 * Default implementation of [NetworkCookieManager].
 */
class NetworkCookieManagerImpl(
    private val configDiskSource: ConfigDiskSource,
    private val cookieDiskSource: CookieDiskSource,
    private val cookieAcquisitionRequestManager: CookieAcquisitionRequestManager,
) : NetworkCookieManager {

    /**
     * Returns the configured cookie domain from the server config, or null if not set.
     */
    private val cookieDomain: String?
        get() = configDiskSource
            .serverConfig
            ?.serverData
            ?.communication
            ?.bootstrap
            ?.takeIf { it.type == BOOTSTRAP_TYPE_SSO_COOKIE_VENDOR }
            ?.cookieDomain

    override fun needsBootstrap(hostname: String): Boolean {
        val result = configDiskSource
            .serverConfig
            ?.serverData
            ?.communication
            ?.bootstrap
            ?.type
            ?.let { bootstrapType ->
                when (bootstrapType) {
                    BOOTSTRAP_TYPE_SSO_COOKIE_VENDOR -> {
                        val resolved = resolveHostname(hostname)
                        cookieDiskSource
                            .getCookieConfig(hostname = resolved)
                            ?.cookies
                            ?.none() != false
                    }

                    else -> false
                }
            }
            ?: false
        Timber.d("needsBootstrap($hostname): $result (cookieDomain=$cookieDomain)")
        return result
    }

    override fun getCookies(hostname: String): List<NetworkCookie> {
        val resolved = resolveHostname(hostname)
        val cookies = cookieDiskSource
            .getCookieConfig(hostname = resolved)
            ?.cookies
            .toNetworkCookieList()
        Timber.d("getCookies($hostname): resolved=$resolved, count=${cookies.size}")
        return cookies
    }

    override fun acquireCookies(hostname: String) {
        Timber.d("acquireCookies($hostname): requesting cookie acquisition")
        cookieAcquisitionRequestManager.setPendingCookieAcquisition(
            CookieAcquisitionRequest(
                hostname = hostname,
            ),
        )
    }

    override fun storeCookies(hostname: String, cookies: Map<String, String>) {
        val resolvedHostname = cookieDomain ?: hostname
        Timber.d(
            "storeCookies($hostname): storing ${cookies.size} cookies under $resolvedHostname",
        )
        cookieDiskSource.storeCookieConfig(
            hostname = resolvedHostname,
            config = CookieConfigurationData(
                hostname = resolvedHostname,
                cookies = cookies.map { (name, value) ->
                    CookieConfigurationData.Cookie(name = name, value = value)
                },
            ),
        )
    }

    /**
     * Resolves the storage key for a given [hostname] by performing domain-suffix fallback.
     *
     * Tries the exact hostname first, then progressively strips the leftmost DNS label
     * until a stored cookie configuration is found or no labels remain. This supports
     * the case where cookies are stored under a parent domain (e.g., "bitwarden.com")
     * but looked up by a subdomain (e.g., "api.bitwarden.com").
     */
    private fun resolveHostname(hostname: String): String {
        var domain = hostname
        while (true) {
            if (cookieDiskSource.getCookieConfig(hostname = domain) != null) {
                if (domain != hostname) {
                    Timber.d("resolveHostname($hostname): resolved to $domain")
                }
                return domain
            }
            val dotIndex = domain.indexOf('.')
            if (dotIndex < 0) {
                Timber.d("resolveHostname($hostname): no stored config found, using original")
                return hostname
            }
            domain = domain.substring(dotIndex + 1)
        }
    }
}
