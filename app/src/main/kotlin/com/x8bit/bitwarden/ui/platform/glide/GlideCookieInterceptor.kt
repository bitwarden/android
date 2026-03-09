package com.x8bit.bitwarden.ui.platform.glide

import com.bitwarden.network.provider.CookieProvider
import okhttp3.Interceptor
import okhttp3.Response

private const val HEADER_COOKIE = "Cookie"

/**
 * Interceptor that attaches cookies to Glide image requests for enterprise environments
 * requiring cookie-based authentication.
 *
 * Unlike [com.bitwarden.network.interceptor.CookieInterceptor], this interceptor only
 * attaches cookies and never throws exceptions or triggers cookie acquisition. Image
 * loading degrades gracefully if cookies are unavailable.
 *
 * @property cookieProvider Provider for retrieving cookies by hostname.
 */
class GlideCookieInterceptor(
    private val cookieProvider: CookieProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val hostname = originalRequest.url.host
        val cookieHeader = cookieProvider
            .getCookies(hostname)
            .joinToString("; ") { "${it.name}=${it.value}" }

        if (cookieHeader.isEmpty()) return chain.proceed(originalRequest)

        val request = originalRequest
            .newBuilder()
            .header(HEADER_COOKIE, cookieHeader)
            .build()
        return chain.proceed(request)
    }
}
