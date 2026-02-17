package com.bitwarden.network.exception

import java.io.IOException

/**
 * Thrown when an HTTP 302 redirect is detected during cookie-authenticated API calls.
 *
 * This exception indicates that the server requires fresh cookies via the SSO cookie vendor
 * endpoint. The service layer should catch this exception and initiate the cookie acquisition
 * flow through the appropriate callback mechanism.
 *
 * @property hostname The server hostname that triggered the redirect.
 */
class CookieRedirectException(
    val hostname: String,
) : IOException("HTTP 302 redirect detected for $hostname.")
