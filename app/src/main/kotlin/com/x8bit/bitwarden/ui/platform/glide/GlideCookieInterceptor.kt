package com.x8bit.bitwarden.ui.platform.glide

import com.bitwarden.network.exception.CookieRedirectException
import com.bitwarden.network.provider.CookieProvider
import okhttp3.Interceptor
import okhttp3.Response

private const val HEADER_COOKIE = "Cookie"
private const val HTTP_302 = 302

/**
 * Interceptor that attaches cookies to Glide image requests for enterprise environments
 * requiring cookie-based authentication.
 *
 * Unlike [com.bitwarden.network.interceptor.CookieInterceptor], this interceptor does not
 * trigger cookie acquisition. It throws [CookieRedirectException] on HTTP 302 responses
 * to prevent Glide from following redirects and caching invalid content.
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

        if (cookieHeader.isEmpty()) return chain.proceed(originalRequest).also(::check302)

        val request = originalRequest
            .newBuilder()
            .header(HEADER_COOKIE, cookieHeader)
            .build()
        return chain.proceed(request).also(::check302)
    }

    private fun check302(response: Response) {
        if (response.code == HTTP_302) {
            response.close()
            throw CookieRedirectException(
                hostname = response.request.url.host,
            )
        }
    }
}
