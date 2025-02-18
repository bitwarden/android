package com.bitwarden.authenticator.data.platform.datasource.network.interceptor

import com.bitwarden.authenticator.data.platform.datasource.network.util.HEADER_KEY_CLIENT_NAME
import com.bitwarden.authenticator.data.platform.datasource.network.util.HEADER_KEY_CLIENT_VERSION
import com.bitwarden.authenticator.data.platform.datasource.network.util.HEADER_KEY_DEVICE_TYPE
import com.bitwarden.authenticator.data.platform.datasource.network.util.HEADER_KEY_USER_AGENT
import com.bitwarden.authenticator.data.platform.datasource.network.util.HEADER_VALUE_CLIENT_NAME
import com.bitwarden.authenticator.data.platform.datasource.network.util.HEADER_VALUE_CLIENT_VERSION
import com.bitwarden.authenticator.data.platform.datasource.network.util.HEADER_VALUE_DEVICE_TYPE
import com.bitwarden.authenticator.data.platform.datasource.network.util.HEADER_VALUE_USER_AGENT
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor responsible for adding various headers to all API requests.
 */
class HeadersInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(
        chain.request()
            .newBuilder()
            .header(HEADER_KEY_USER_AGENT, HEADER_VALUE_USER_AGENT)
            .header(HEADER_KEY_CLIENT_NAME, HEADER_VALUE_CLIENT_NAME)
            .header(HEADER_KEY_CLIENT_VERSION, HEADER_VALUE_CLIENT_VERSION)
            .header(HEADER_KEY_DEVICE_TYPE, HEADER_VALUE_DEVICE_TYPE)
            .build(),
    )
}
