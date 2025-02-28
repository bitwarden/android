package com.bitwarden.authenticator.data.platform.datasource.network.interceptor

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * A [Interceptor] that optionally takes the current base URL of a request and replaces it with
 * the currently set [baseUrl]
 */
class BaseUrlInterceptor : Interceptor {

    /**
     * The base URL to use as an override, or `null` if no override should be performed.
     */
    var baseUrl: String? = null
        set(value) {
            field = value
            baseHttpUrl = baseUrl?.let { requireNotNull(it.toHttpUrlOrNull()) }
        }

    private var baseHttpUrl: HttpUrl? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // If no base URL is set, we can simply skip
        val base = baseHttpUrl ?: return chain.proceed(request)

        // Update the base URL used.
        return chain.proceed(
            request
                .newBuilder()
                .url(
                    request
                        .url
                        .replaceBaseUrlWith(base),
                )
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
