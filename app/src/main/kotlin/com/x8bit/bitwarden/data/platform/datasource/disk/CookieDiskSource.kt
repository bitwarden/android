package com.x8bit.bitwarden.data.platform.datasource.disk

import com.x8bit.bitwarden.data.platform.datasource.disk.model.CookieConfigurationData

/**
 * Disk source for cookie persistence.
 */
interface CookieDiskSource {

    /**
     * Gets cookie configuration for a specific [hostname].
     *
     * @param hostname The server hostname to retrieve configuration for.
     * @return The [CookieConfigurationData] if found, or null if no cookies stored.
     */
    fun getCookieConfig(hostname: String): CookieConfigurationData?

    /**
     * Stores cookie [config] for the given [hostname].
     *
     * @param hostname The server hostname to associate with this configuration.
     * @param config The [CookieConfigurationData] to persist.
     */
    fun storeCookieConfig(hostname: String, config: CookieConfigurationData)

    /**
     * Deletes cookie configuration for the given [hostname].
     *
     * @param hostname The server hostname to delete configuration for.
     */
    fun deleteCookieConfig(hostname: String)
}
