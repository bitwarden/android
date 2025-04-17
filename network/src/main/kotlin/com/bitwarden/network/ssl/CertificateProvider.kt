package com.bitwarden.network.ssl

import java.net.Socket
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate

/**
 * Provides certificates for SSL connections.
 */
interface CertificateProvider {
    /**
     * Selects the alias of the client certificate to use.
     */
    fun chooseClientAlias(
        keyType: Array<out String>?,
        issuers: Array<out Principal>?,
        socket: Socket?,
    ): String

    /**
     * Returns the certificate chain for the given alias.
     */
    fun getCertificateChain(alias: String?): Array<X509Certificate>?

    /**
     * Returns the private key for the given alias.
     */
    fun getPrivateKey(alias: String?): PrivateKey?
}
