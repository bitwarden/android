package com.bitwarden.network.interceptor

import okhttp3.Authenticator
import okhttp3.Cache
import okhttp3.Call
import okhttp3.CertificatePinner
import okhttp3.Connection
import okhttp3.ConnectionPool
import okhttp3.CookieJar
import okhttp3.Dns
import okhttp3.EventListener
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import java.net.Proxy
import java.net.ProxySelector
import java.util.concurrent.TimeUnit
import javax.net.SocketFactory
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

/**
 * Helper class for implementing a [Interceptor.Chain] in a way that a [Request] passed in to
 * [proceed] will be returned in a valid [Response] object that can be queried. This wrapping is
 * performed by the [responseProvider].
 */
class FakeInterceptorChain(
    private val request: Request,
    private val responseProvider: (Request) -> Response = DEFAULT_RESPONSE_PROVIDER,
) : Interceptor.Chain {
    override val authenticator: Authenticator get() = notImplemented()
    override val cache: Cache get() = notImplemented()
    override val certificatePinner: CertificatePinner get() = notImplemented()
    override val connectionPool: ConnectionPool get() = notImplemented()
    override val cookieJar: CookieJar get() = notImplemented()
    override val dns: Dns get() = notImplemented()
    override val eventListener: EventListener get() = notImplemented()
    override val followRedirects: Boolean get() = notImplemented()
    override val followSslRedirects: Boolean get() = notImplemented()
    override val hostnameVerifier: HostnameVerifier get() = notImplemented()
    override val proxy: Proxy get() = notImplemented()
    override val proxyAuthenticator: Authenticator get() = notImplemented()
    override val proxySelector: ProxySelector get() = notImplemented()
    override val retryOnConnectionFailure: Boolean get() = notImplemented()
    override val socketFactory: SocketFactory get() = notImplemented()
    override val sslSocketFactoryOrNull: SSLSocketFactory get() = notImplemented()
    override val x509TrustManagerOrNull: X509TrustManager get() = notImplemented()

    override fun withAuthenticator(
        authenticator: Authenticator,
    ): Interceptor.Chain = notImplemented()

    override fun withCache(cache: Cache?): Interceptor.Chain = notImplemented()

    override fun withCertificatePinner(
        certificatePinner: CertificatePinner,
    ): Interceptor.Chain = notImplemented()

    override fun withConnectionPool(
        connectionPool: ConnectionPool,
    ): Interceptor.Chain = notImplemented()

    override fun withCookieJar(cookieJar: CookieJar): Interceptor.Chain = notImplemented()

    override fun withDns(dns: Dns): Interceptor.Chain = notImplemented()

    override fun withHostnameVerifier(
        hostnameVerifier: HostnameVerifier,
    ): Interceptor.Chain = notImplemented()

    override fun withProxy(proxy: Proxy?): Interceptor.Chain = notImplemented()

    override fun withProxyAuthenticator(
        proxyAuthenticator: Authenticator,
    ): Interceptor.Chain = notImplemented()

    override fun withProxySelector(
        proxySelector: ProxySelector,
    ): Interceptor.Chain = notImplemented()

    override fun withRetryOnConnectionFailure(
        retryOnConnectionFailure: Boolean,
    ): Interceptor.Chain = notImplemented()

    override fun withSocketFactory(
        socketFactory: SocketFactory,
    ): Interceptor.Chain = notImplemented()

    override fun withSslSocketFactory(
        sslSocketFactory: SSLSocketFactory?,
        x509TrustManager: X509TrustManager?,
    ): Interceptor.Chain = notImplemented()

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
