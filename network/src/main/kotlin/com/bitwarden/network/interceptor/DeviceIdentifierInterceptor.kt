package com.bitwarden.network.interceptor

import com.bitwarden.network.provider.AppIdProvider
import com.bitwarden.network.util.HEADER_KEY_AUTHORIZATION
import com.bitwarden.network.util.HEADER_KEY_DEVICE_IDENTIFIER
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor responsible for identifying the device on API requests that carry no access token,
 * since the server cannot determine it from the token. Requests that already have an
 * `Authorization` header are left untouched.
 */
internal class DeviceIdentifierInterceptor(
    private val appIdProvider: AppIdProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.header(name = HEADER_KEY_AUTHORIZATION) != null) {
            return chain.proceed(request = request)
        }
        return chain.proceed(
            request = request
                .newBuilder()
                .header(
                    name = HEADER_KEY_DEVICE_IDENTIFIER,
                    value = appIdProvider.uniqueAppId,
                )
                .build(),
        )
    }
}
