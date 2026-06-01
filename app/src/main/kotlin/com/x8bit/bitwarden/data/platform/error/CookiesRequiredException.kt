package com.x8bit.bitwarden.data.platform.error

import java.io.IOException

/**
 * Thrown when SDK requires cookie acquisition before API call can proceed.
 *
 * @property hostname The server hostname requiring cookie acquisition.
 */
class CookiesRequiredException(
    val hostname: String,
) : IOException("Cookie acquisition required for $hostname")
