package com.bitwarden.network.interceptor

import com.bitwarden.network.exception.CookieRedirectException
import com.bitwarden.network.provider.CookieProvider
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber

private const val HEADER_COOKIE = "Cookie"
private const val HTTP_REDIRECT = 302
private const val PATH_CONFIG = "/config"
private const val PATH_SSO_COOKIE_VENDOR = "/sso-cookie-vendor"

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
 * - Skips excluded paths that should not receive cookies (`/config`, `/sso-cookie-vendor`)
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
        if (EXCLUDED_PATHS.any { path.endsWith(it) }) {
            Timber.d("Cookie interceptor skipping excluded path: $path")
            return chain.proceed(originalRequest)
        }

        val hostname = url.host

        // Preempt: if cookies are required but not yet available, trigger acquisition
        // immediately rather than making a doomed request.
        if (cookieProvider.needsBootstrap(hostname)) {
            Timber.d("Cookie bootstrap required for $hostname, triggering acquisition")
            cookieProvider.acquireCookies(hostname)
            throw CookieRedirectException(hostname = hostname)
        }

        val request = originalRequest.withCookies(hostname)
        val response = chain.proceed(request)

        if (response.code != HTTP_REDIRECT) {
            return response
        }

        // Close the response body to release the connection back to the pool
        // before throwing to cancel the current request.
        Timber.d("Received 302 redirect for $hostname, triggering cookie re-acquisition")
        response.close()
        cookieProvider.acquireCookies(hostname)
        throw CookieRedirectException(
            hostname = hostname,
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
