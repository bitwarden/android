package com.bitwarden.network.interceptor

import com.bitwarden.network.provider.CustomHeadersProvider
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor responsible for attaching the user's custom headers to requests sent to a
 * self-hosted environment.
 *
 * The [CustomHeadersProvider] scopes the headers to the environment's hosts, so requests to
 * third-party hosts (e.g. Have I Been Pwned) are left untouched. This must be installed as a
 * network interceptor so the header values, which may contain credentials such as Cloudflare
 * Access service tokens, are never seen by the application-level logging interceptor.
 */
class CustomHeadersInterceptor(
    private val customHeadersProvider: CustomHeadersProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val customHeaders = customHeadersProvider.getCustomHeaders(url = request.url.toString())
        if (customHeaders.isEmpty()) return chain.proceed(request)
        return chain.proceed(
            request
                .newBuilder()
                .apply { customHeaders.forEach { (name, value) -> header(name, value) } }
                .build(),
        )
    }
}
