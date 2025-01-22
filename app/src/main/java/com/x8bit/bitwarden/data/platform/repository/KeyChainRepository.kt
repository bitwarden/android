package com.x8bit.bitwarden.data.platform.repository

import java.security.PrivateKey
import java.security.cert.X509Certificate

/**
 * Repository for accessing the KeyChain.
 */
interface KeyChainRepository {

    /**
     * Returns the private key.
     */
    fun getPrivateKey(): PrivateKey?

    /**
     * Returns the certificate chain.
     */
    fun getCertificateChain(): Array<X509Certificate>?
}
