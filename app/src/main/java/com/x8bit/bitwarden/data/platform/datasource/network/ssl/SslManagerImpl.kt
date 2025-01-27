package com.x8bit.bitwarden.data.platform.datasource.network.ssl

import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri
import com.x8bit.bitwarden.data.platform.datasource.disk.model.MutualTlsCertificate
import com.x8bit.bitwarden.data.platform.datasource.disk.model.MutualTlsKeyHost
import com.x8bit.bitwarden.data.platform.manager.KeyManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import java.net.Socket
import java.security.KeyStore
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509ExtendedKeyManager

/**
 * Primary implementation of [SslManager].
 */
class SslManagerImpl(
    private val keyManager: KeyManager,
    private val environmentRepository: EnvironmentRepository,
) : SslManager {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val mutualTlsCertificate: MutualTlsCertificate?
        get() {
            val keyUri = environmentRepository
                .environment
                .environmentUrlData
                .keyUri
                ?.toUri()
                ?: return null

            val host = MutualTlsKeyHost
                .entries
                .find { it.name == keyUri.authority }
                ?: return null

            val alias = keyUri.path
                ?.trim('/')
                ?.takeUnless { it.isEmpty() }
                ?: return null

            return keyManager.getMutualTlsCertificateChain(
                alias = alias,
                host = host,
            )
        }

    override val trustManagers: Array<TrustManager>
        get() = TrustManagerFactory
            .getInstance(TrustManagerFactory.getDefaultAlgorithm())
            .apply { init(null as KeyStore?) }
            .trustManagers

    override val sslContext: SSLContext?
        get() =
            mutualTlsCertificate
                ?.let { certificate ->
                    environmentRepository
                        .environment
                        .environmentUrlData
                        .keyUri
                        ?.toUri()
                        ?.let {
                            SSLContext.getInstance("TLS")
                                .apply {
                                    init(
                                        arrayOf(X509ExtendedKeyManagerImpl(certificate)),
                                        trustManagers,
                                        null,
                                    )
                                }
                        }
                }

    private class X509ExtendedKeyManagerImpl(
        private val mutualTlsCertificate: MutualTlsCertificate,
    ) : X509ExtendedKeyManager() {
        override fun chooseClientAlias(
            keyType: Array<out String>?,
            issuers: Array<out Principal>?,
            socket: Socket?,
        ): String = mutualTlsCertificate.alias

        override fun getCertificateChain(
            alias: String?,
        ): Array<X509Certificate>? =
            mutualTlsCertificate
                .certificateChain
                .toTypedArray()

        override fun getPrivateKey(alias: String?): PrivateKey? =
            mutualTlsCertificate
                .privateKey

        //region Unused server side methods
        override fun getServerAliases(
            alias: String?,
            issuers: Array<out Principal>?,
        ): Array<String> {
            return arrayOf()
        }

        override fun getClientAliases(
            keyType: String?,
            issuers: Array<out Principal>?,
        ): Array<String> = emptyArray()

        override fun chooseServerAlias(
            alias: String?,
            issuers: Array<out Principal>?,
            socket: Socket?,
        ): String {
            return ""
        }
        //endregion Unused server side methods
    }
}
