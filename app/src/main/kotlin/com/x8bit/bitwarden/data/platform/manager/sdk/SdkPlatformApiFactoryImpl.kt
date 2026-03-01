package com.x8bit.bitwarden.data.platform.manager.sdk

import com.bitwarden.servercommunicationconfig.ServerCommunicationConfigPlatformApi
import com.x8bit.bitwarden.data.platform.manager.CookieAcquisitionRequestManager
import com.x8bit.bitwarden.data.platform.manager.sdk.platformapi.ServerCommunicationConfigPlatformApiImpl

/**
 * Factory for creating and managing sdk platform api's.
 */
class SdkPlatformApiFactoryImpl(
    private val serverCommConfigManager: CookieAcquisitionRequestManager,
) : SdkPlatformApiFactory {
    /**
     * Retrieves or creates a [ServerCommunicationConfigPlatformApi] for use with the Bitwarden SDK.
     */
    override fun getServerCommunicationConfigPlatformApi(): ServerCommunicationConfigPlatformApi =
        ServerCommunicationConfigPlatformApiImpl(
            serverCommConfigManager = serverCommConfigManager,
        )
}
