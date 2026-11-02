package com.x8bit.bitwarden.ui.platform.image

import okhttp3.Interceptor
import okhttp3.Response

private const val HEADER_CACHE_CONTROL = "Cache-Control"
private const val NO_STORE = "no-store"

/**
 * Network interceptor that limits the HTTP cache to error responses.
 *
 * Coil already caches successful images on disk, but it never caches errors, so an icon the
 * server lacks (HTTP 404) is requested again on every load. Marking successful responses as
 * `no-store` leaves the OkHttp cache to store only the errors, following the server's own
 * `Cache-Control` freshness, without keeping a second copy of every icon.
 */
class ImageNotFoundCacheInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (!response.isSuccessful) return response

        return response
            .newBuilder()
            .header(HEADER_CACHE_CONTROL, NO_STORE)
            .build()
    }
}
