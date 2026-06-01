package com.x8bit.bitwarden.data.platform.repository.util

import com.bitwarden.servercommunicationconfig.AcquiredCookie
import com.x8bit.bitwarden.data.platform.datasource.disk.model.CookieConfigurationData

/**
 * Converts a list of [CookieConfigurationData.Cookie] to a list of [AcquiredCookie].
 */
fun List<CookieConfigurationData.Cookie>.toAcquiredCookiesList() = this
    .map { it.toAcquiredCookie() }

/**
 * Converts a [CookieConfigurationData.Cookie] to an [AcquiredCookie].
 */
fun CookieConfigurationData.Cookie.toAcquiredCookie() = AcquiredCookie(
    name = this.name,
    value = this.value,
)
