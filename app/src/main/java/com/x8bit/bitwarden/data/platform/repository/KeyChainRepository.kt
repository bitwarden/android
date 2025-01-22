package com.x8bit.bitwarden.data.platform.repository

import android.app.Activity
import java.security.PrivateKey
import java.security.cert.X509Certificate

/**
 * Repository for accessing the KeyChain.
 */
interface KeyChainRepository {
    /**
     * Chooses a private key alias.
     */
    fun choosePrivateKeyAlias(activity: Activity, callback: ChoosePrivateKeyAliasCallback)

    /**
     * Returns the private key.
     */
    fun getPrivateKey(): PrivateKey?

    /**
     * Returns the certificate chain.
     */
    fun getCertificateChain(): Array<X509Certificate>?
}
