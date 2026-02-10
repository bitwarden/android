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

    override suspend fun get(hostname: String): ServerCommunicationConfig? {
        val serverCommunicationConfig = configDiskSource
            .serverConfig
            ?.serverData
            ?.communication
            ?: return null

        if (serverCommunicationConfig.bootstrap.type != "ssoCookieVendor") {
            return ServerCommunicationConfig(
                bootstrap = BootstrapConfig.Direct,
            )
        }

        val acquiredCookies = cookieDiskSource
            .getCookieConfig(hostname)
            ?.cookies
            ?.toAcquiredCookiesList()

        return ServerCommunicationConfig(
            bootstrap = BootstrapConfig.SsoCookieVendor(
                v1 = SsoCookieVendorConfig(
                    idpLoginUrl = serverCommunicationConfig.bootstrap.idpLoginUrl,
                    cookieName = serverCommunicationConfig.bootstrap.cookieName,
                    cookieDomain = serverCommunicationConfig.bootstrap.cookieDomain,
                    cookieValue = acquiredCookies,
                ),
            ),
        )
    }

    override suspend fun save(hostname: String, config: ServerCommunicationConfig) =
        when (val bootstrapConfig = config.bootstrap) {
            is BootstrapConfig.SsoCookieVendor -> {
                // Only store cookies from [config]. The communication config is synced with the
                // server (api/config), which takes precedence over the local configuration.
                cookieDiskSource.storeCookieConfig(
                    hostname = hostname,
                    config = CookieConfigurationData(
                        hostname = hostname,
                        cookies = bootstrapConfig.v1.cookieValue
                            ?.toConfigurationDataCookies()
                            .orEmpty(),
                    ),
                )
            }

            BootstrapConfig.Direct -> {
                // Clear any existing cookie configuration now that the communication config
                // has been updated.
                cookieDiskSource.deleteCookieConfig(hostname)
            }
        }
}
