package com.bitwarden.network.interceptor

import com.bitwarden.network.exception.CookieRedirectException
import com.bitwarden.network.provider.CookieProvider
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

private const val HEADER_COOKIE = "Cookie"
private const val HEADER_LOCATION = "Location"
private const val HTTP_REDIRECT = 302
private const val PATH_CONFIG = "/api/config"
private const val PATH_SSO_COOKIE_VENDOR = "/api/sso-cookie-vendor"

/**
 * Paths that should not receive cookie handling.
 */
private val EXCLUDED_PATHS = listOf(
    PATH_CONFIG,
    PATH_SSO_COOKIE_VENDOR,
)

/**
 * Interceptor responsible for attaching ELB cookies to API requests and detecting
 * HTTP 302 redirects that indicate cookie acquisition is required.
 *
 * This interceptor supports the SSO cookie vending authentication flow where enterprise
 * customers may need to obtain session cookies from an identity provider before accessing
 * Bitwarden API endpoints behind a load balancer.
 *
 * **Behavior:**
 * - Skips excluded paths that should not receive cookies (`/api/config`, `/api/sso-cookie-vendor`)
 * - Preempts requests when cookie bootstrap is needed by throwing [CookieRedirectException]
 *   before the request is sent, avoiding a wasted round-trip
 * - Attaches available cookies to the request Cookie header
 * - Detects HTTP 302 responses and throws [CookieRedirectException] to trigger re-acquisition
 *   when cookies may have expired
 *
 * @property cookieProvider Provider for retrieving and managing cookies.
 */
internal class CookieInterceptor(
    private val cookieProvider: CookieProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url
        val path = url.encodedPath

        // Skip excluded paths that should not receive cookies.
        if (EXCLUDED_PATHS.any { path.startsWith(it) }) {
            return chain.proceed(originalRequest)
        }

        val hostname = url.host

        // Preempt: if cookies are required but not yet available, trigger acquisition
        // immediately rather than making a doomed request.
        if (cookieProvider.needsBootstrap(hostname)) {
            cookieProvider.acquireCookies(hostname)
            throw CookieRedirectException(hostname = hostname, location = null)
        }

        val request = originalRequest.withCookies(hostname)
        val response = chain.proceed(request)

        // Return the response if it is not a redirect or does not contain
        // a Location header.
        val location = response.header(HEADER_LOCATION)
        if (response.code != HTTP_REDIRECT || location == null) {
            return response
        }

        // Close the response body to release the connection back to the pool
        // before throwing to cancel the current request.
        response.close()
        cookieProvider.acquireCookies(hostname)
        throw CookieRedirectException(
            hostname = hostname,
            location = location,
        )
    }

    /**
     * Attaches available cookies to the request for the given [hostname].
     */
    private fun Request.withCookies(hostname: String): Request {
        val cookieHeader = cookieProvider
            .getCookies(hostname)
            .joinToString("; ") { "${it.name}=${it.value}" }

        if (cookieHeader.isEmpty()) return this

        return newBuilder()
            .header(HEADER_COOKIE, cookieHeader)
            .build()
    }
}
