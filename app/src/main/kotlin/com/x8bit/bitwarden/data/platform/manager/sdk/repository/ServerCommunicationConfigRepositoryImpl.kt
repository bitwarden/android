package com.x8bit.bitwarden.data.platform.manager.sdk.repository

import com.bitwarden.data.datasource.disk.ConfigDiskSource
import com.bitwarden.sdk.ServerCommunicationConfigRepository
import com.bitwarden.servercommunicationconfig.BootstrapConfig
import com.bitwarden.servercommunicationconfig.ServerCommunicationConfig
import com.bitwarden.servercommunicationconfig.SsoCookieVendorConfig
import com.x8bit.bitwarden.data.platform.datasource.disk.CookieDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.model.CookieConfigurationData
import com.x8bit.bitwarden.data.platform.repository.util.toAcquiredCookiesList
import com.x8bit.bitwarden.data.platform.repository.util.toConfigurationDataCookies

/**
 * Implementation of SDK's [ServerCommunicationConfigRepository].
 * Bridges the SDK's storage interface to the application's [CookieDiskSource].
 *
 * @property cookieDiskSource The disk source for persisting cookie configurations.
 */
class ServerCommunicationConfigRepositoryImpl(
    private val cookieDiskSource: CookieDiskSource,
    private val configDiskSource: ConfigDiskSource,
) : ServerCommunicationConfigRepository {

    override suspend fun get(domain: String): ServerCommunicationConfig? {
        val serverData = configDiskSource.serverConfig?.serverData
        val serverCommunicationConfig = serverData?.communication ?: return null

        if (serverCommunicationConfig.bootstrap.type != "ssoCookieVendor") {
            return ServerCommunicationConfig(
                bootstrap = BootstrapConfig.Direct,
            )
        }

        // We return null here since we do not have the appropriate data to complete the
        // transaction. This will trigger a cookie acquisition with the server.
        val vaultUrl = serverData.environment?.vaultUrl ?: return null
        val cookieName = serverCommunicationConfig.bootstrap.cookieName ?: return null
        val cookieDomain = serverCommunicationConfig.bootstrap.cookieDomain ?: return null
        val acquiredCookies = cookieDiskSource
            .getCookieConfig(hostname = domain)
            ?.cookies
            ?.toAcquiredCookiesList()

        return ServerCommunicationConfig(
            bootstrap = BootstrapConfig.SsoCookieVendor(
                v1 = SsoCookieVendorConfig(
                    idpLoginUrl = serverCommunicationConfig.bootstrap.idpLoginUrl,
                    vaultUrl = vaultUrl,
                    cookieName = cookieName,
                    cookieDomain = cookieDomain,
                    cookieValue = acquiredCookies,
                ),
            ),
        )
    }

    override suspend fun save(domain: String, config: ServerCommunicationConfig) =
        when (val bootstrapConfig = config.bootstrap) {
            is BootstrapConfig.SsoCookieVendor -> {
                // Only store cookies from [config]. The communication config is synced with the
                // server (api/config), which takes precedence over the local configuration.
                cookieDiskSource.storeCookieConfig(
                    hostname = domain,
                    config = CookieConfigurationData(
                        hostname = domain,
                        cookies = bootstrapConfig.v1.cookieValue
                            ?.toConfigurationDataCookies()
                            .orEmpty(),
                    ),
                )
            }

            BootstrapConfig.Direct -> {
                // Clear any existing cookie configuration now that the communication config
                // has been updated.
                cookieDiskSource.storeCookieConfig(hostname = domain, config = null)
            }
        }
}
