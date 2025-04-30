package com.bitwarden.network.util

/**
 * An enum that represents HTTP error codes that we may need to parse for specific responses.
 */
internal enum class NetworkErrorCode(
    val code: Int,
) {
    BAD_REQUEST(code = 400),
    TOO_MANY_REQUESTS(code = 429),
}
