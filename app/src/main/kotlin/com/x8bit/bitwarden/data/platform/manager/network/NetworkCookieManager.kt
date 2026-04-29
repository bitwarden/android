package com.x8bit.bitwarden.data.platform.manager.network

import com.bitwarden.network.provider.CookieProvider

/**
 * A manager class for handling cookies.
 */
interface NetworkCookieManager : CookieProvider {

    /**
     * Stores acquired cookies for the given [hostname].
     *
     * @param hostname The hostname to associate with the cookies.
     * @param cookies A map of cookie name to cookie value.
     */
    fun storeCookies(hostname: String, cookies: Map<String, String>)
}
