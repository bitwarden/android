package com.bitwarden.network.ssl

import okhttp3.OkHttpClient
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Creates an [OkHttpClient] configured with mTLS support using this [CertificateProvider].
 *
 * The returned client will present the client certificate from this provider during TLS
 * handshakes, allowing requests to pass through mTLS checks.
 */
fun CertificateProvider.createMtlsOkHttpClient(): OkHttpClient =
    OkHttpClient.Builder()
        .configureSsl(certificateProvider = this)
        .build()

/**
 * Configures the [OkHttpClient.Builder] to use the a `SSLSocketFactory` as provided by the
 * [CertificateProvider].
 */
fun OkHttpClient.Builder.configureSsl(
    certificateProvider: CertificateProvider,
): OkHttpClient.Builder {
    val trustManagers = sslTrustManagers
    val sslContext = certificateProvider.createSslContext(trustManagers = trustManagers)
    return sslSocketFactory(
        sslContext.socketFactory,
        trustManagers.first() as X509TrustManager,
    )
}

/**
 * Creates an [SSLContext] configured with mTLS support using this [CertificateProvider].
 *
 * The returned SSLContext will present the client certificate from this provider during
 * TLS handshakes, enabling mutual TLS authentication.
 */
private fun CertificateProvider.createSslContext(
    trustManagers: Array<TrustManager>,
): SSLContext = SSLContext.getInstance("TLS").apply {
    init(
        arrayOf(
            BitwardenX509ExtendedKeyManager(certificateProvider = this@createSslContext),
        ),
        trustManagers,
        null,
    )
}

/**
 * Creates default [TrustManager]s for verifying server certificates.
 *
 * Uses the system's default trust anchors (trusted CA certificates).
 */
private val sslTrustManagers: Array<TrustManager>
    get() = TrustManagerFactory
        .getInstance(TrustManagerFactory.getDefaultAlgorithm())
        .apply { init(null as KeyStore?) }
        .trustManagers
