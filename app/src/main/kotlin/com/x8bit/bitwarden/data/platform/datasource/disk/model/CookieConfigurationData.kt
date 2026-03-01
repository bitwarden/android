package com.x8bit.bitwarden.data.platform.datasource.disk.model

import kotlinx.serialization.Serializable

/**
 * Simple domain model for cookie storage.
 *
 * @property hostname The server hostname this configuration applies to.
 * @property cookies The list of cookies for this server configuration.
 */
@Serializable
data class CookieConfigurationData(
    val hostname: String,
    val cookies: List<Cookie>,
) {
    /**
     * Simple domain model for a cookie.
     *
     * @property name The cookie name.
     * @property value The cookie value.
     */
    @Serializable
    data class Cookie(
        val name: String,
        val value: String,
    )
}
