package com.bitwarden.authenticator.data.platform.datasource.network.interceptor

import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Helper class for implementing a [Interceptor.Chain] in a way that a [Request] passed in to
 * [proceed] will be returned in a valid [Response] object that can be queried. This wrapping is
 * performed by the [responseProvider].
 */
class FakeInterceptorChain(
    private val request: Request,
    private val responseProvider: (Request) -> Response = DEFAULT_RESPONSE_PROVIDER,
) : Interceptor.Chain {
    override fun request(): Request = request

    override fun proceed(request: Request): Response = responseProvider(request)

    override fun connection(): Connection = notImplemented()

    override fun call(): Call = notImplemented()

    override fun connectTimeoutMillis(): Int = notImplemented()

    override fun withConnectTimeout(
        timeout: Int,
        unit: TimeUnit,
    ): Interceptor.Chain = notImplemented()

    override fun readTimeoutMillis(): Int = notImplemented()

    override fun withReadTimeout(
        timeout: Int,
        unit: TimeUnit,
    ): Interceptor.Chain = notImplemented()

    override fun writeTimeoutMillis(): Int = notImplemented()

    override fun withWriteTimeout(
        timeout: Int,
        unit: TimeUnit,
    ): Interceptor.Chain = notImplemented()

    private fun notImplemented(): Nothing {
        throw NotImplementedError("This is not yet required by tests")
    }

    companion object {
        /**
         * A default response provider that provides a basic successful response. This is useful
         * when the details of the response are not as important as retrieving the [Request] that
         * was used to build it.
         */
        val DEFAULT_RESPONSE_PROVIDER: (Request) -> Response = { request ->
            Response
                .Builder()
                .code(200)
                .message("OK")
                .protocol(Protocol.HTTP_1_1)
                .request(request)
                .build()
        }
    }
}
