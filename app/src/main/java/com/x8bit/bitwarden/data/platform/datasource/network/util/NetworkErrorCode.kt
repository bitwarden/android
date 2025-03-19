package com.x8bit.bitwarden.data.platform.datasource.network.util

/**
 * An enum that represents HTTP error codes that we may need to parse for specific responses.
 */
enum class NetworkErrorCode(
    val code: Int,
) {
    BAD_REQUEST(code = 400),
    TOO_MANY_REQUESTS(code = 429),
}
