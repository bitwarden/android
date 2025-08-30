package com.bitwarden.network.interceptor

import com.bitwarden.network.util.HEADER_KEY_CLIENT_NAME
import com.bitwarden.network.util.HEADER_KEY_CLIENT_VERSION
import com.bitwarden.network.util.HEADER_KEY_DEVICE_TYPE
import com.bitwarden.network.util.HEADER_KEY_USER_AGENT
import com.bitwarden.network.util.HEADER_VALUE_DEVICE_TYPE
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor responsible for adding various headers to all API requests.
 */
internal class HeadersInterceptor(
    private val userAgent: String,
    private val clientName: String,
    private val clientVersion: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(
        chain.request()
            .newBuilder()
            .header(HEADER_KEY_USER_AGENT, userAgent)
            .header(HEADER_KEY_CLIENT_NAME, clientName)
            .header(HEADER_KEY_CLIENT_VERSION, clientVersion)
            .header(HEADER_KEY_DEVICE_TYPE, HEADER_VALUE_DEVICE_TYPE)
            .build(),
    )
}
