package com.bitwarden.network.provider

import com.bitwarden.network.model.NetworkCookie

/**
 * Provider for server communication cookies used in ELB cookie vending authentication flow.
 *
 * This provider supports the cookie acquisition workflow where enterprise customers using
 * SSO may need to obtain session cookies from an identity provider before accessing
 * Bitwarden API endpoints behind a load balancer.
 */
interface CookieProvider {

    /**
     * Determines if the given [hostname] requires cookie bootstrap.
     *
     * Returns `true` when the server configuration indicates SSO cookie vending is required
     * but no cookies have been acquired yet. Used to preempt API requests that would
     * fail without cookies.
     *
     * @param hostname The hostname to check for bootstrap requirements.
     * @return `true` if cookies need to be acquired before making requests, `false` otherwise.
     */
    fun needsBootstrap(hostname: String): Boolean

    /**
     * Retrieves cookies for the given [hostname].
     *
     * @param hostname The hostname for which to retrieve cookies.
     * @return A list of [NetworkCookie] for the hostname, or empty list if none available.
     */
    fun getCookies(hostname: String): List<NetworkCookie>

    /**
     * Signals that cookie acquisition is required for the given [hostname].
     *
     * The triggering API request will be canceled.
     *
     * @param hostname The hostname requiring cookies.
     */
    fun acquireCookies(hostname: String)
}
