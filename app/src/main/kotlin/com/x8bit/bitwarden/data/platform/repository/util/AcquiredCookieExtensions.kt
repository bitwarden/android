package com.x8bit.bitwarden.data.platform.repository.util

import com.bitwarden.servercommunicationconfig.AcquiredCookie
import com.x8bit.bitwarden.data.platform.datasource.disk.model.CookieConfigurationData

/**
 * Converts a list of [AcquiredCookie] to a list of [CookieConfigurationData.Cookie].
 */
fun List<AcquiredCookie>.toConfigurationDataCookies(): List<CookieConfigurationData.Cookie> = this
    .map { it.toConfigurationCookie() }

/**
 * Converts an [AcquiredCookie] to a [CookieConfigurationData.Cookie].
 */
fun AcquiredCookie.toConfigurationCookie(): CookieConfigurationData.Cookie =
    CookieConfigurationData.Cookie(
        name = name,
        value = value,
    )
