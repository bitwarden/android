package com.x8bit.bitwarden.data.platform.datasource.network.ssl

import com.bitwarden.core.annotation.OmitFromCoverage
import com.bitwarden.network.ssl.CertificateProvider
import java.net.Socket
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.net.ssl.X509ExtendedKeyManager

/**
 * Implementation of [X509ExtendedKeyManager] that delegates client callbacks to
 * [CertificateProvider] and provides default implementations for server callbacks.
 */
@OmitFromCoverage
internal class BitwardenX509ExtendedKeyManager(
    private val certificateProvider: CertificateProvider,
) : X509ExtendedKeyManager() {
    override fun chooseClientAlias(
        keyType: Array<out String>?,
        issuers: Array<out Principal>?,
        socket: Socket?,
    ): String = certificateProvider.chooseClientAlias(
        keyType = keyType,
        issuers = issuers,
        socket = socket,
    )

    override fun getCertificateChain(
        alias: String?,
    ): Array<X509Certificate>? = certificateProvider.getCertificateChain(alias)

    override fun getPrivateKey(alias: String?): PrivateKey? =
        certificateProvider.getPrivateKey(alias)

    //region Unused server side methods
    override fun getServerAliases(
        alias: String?,
        issuers: Array<out Principal>?,
    ): Array<String> = emptyArray()

    override fun getClientAliases(
        keyType: String?,
        issuers: Array<out Principal>?,
    ): Array<String> = emptyArray()

    override fun chooseServerAlias(
        alias: String?,
        issuers: Array<out Principal>?,
        socket: Socket?,
    ): String = ""
    //endregion Unused server side methods
}
