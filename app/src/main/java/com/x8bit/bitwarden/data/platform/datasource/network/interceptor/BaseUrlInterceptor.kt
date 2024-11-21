package com.x8bit.bitwarden.data.platform.datasource.network.interceptor

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * An [Interceptor] that optionally takes the current base URL of a request and replaces it with
 * the currently set base URL from the [baseUrlProvider].
 */
class BaseUrlInterceptor(
    private val baseUrlProvider: () -> String?,
) : Interceptor {

    private val baseHttpUrl: HttpUrl? get() = baseUrlProvider()?.toHttpUrlOrNull()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // If no base URL is set, we can simply skip
        val base = baseHttpUrl ?: return chain.proceed(request = request)

        // Update the base URL used.
        return chain.proceed(
            request = request
                .newBuilder()
                .url(url = request.url.replaceBaseUrlWith(baseUrl = base))
                .build(),
        )
    }
}

/**
 * Given a [HttpUrl], replaces the existing base URL with the given [baseUrl].
 */
private fun HttpUrl.replaceBaseUrlWith(
    baseUrl: HttpUrl,
) = baseUrl
    .newBuilder()
    .addEncodedPathSegments(
        this
            .encodedPathSegments
            .joinToString(separator = "/"),
    )
    .encodedQuery(this.encodedQuery)
    .build()
