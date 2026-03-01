package com.x8bit.bitwarden.data.platform.manager.model

/**
 * Represents pending cookie acquisition request for a specific hostname.
 *
 * @property hostname The server hostname requiring cookies.
 */
data class CookieAcquisitionRequest(
    val hostname: String,
)
