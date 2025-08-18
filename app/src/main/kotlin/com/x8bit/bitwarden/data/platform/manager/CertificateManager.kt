package com.x8bit.bitwarden.data.platform.manager

import com.bitwarden.network.ssl.CertificateProvider
import com.x8bit.bitwarden.data.platform.datasource.disk.model.MutualTlsKeyHost
import com.x8bit.bitwarden.data.platform.manager.model.ImportPrivateKeyResult

/**
 * Primary access point for disk information related to key data.
 */
interface CertificateManager : CertificateProvider {

    /**
     * Returns a list of aliases for all mTLS keys stored in the application KeyStore.
     */
    fun getMutualTlsKeyAliases(): List<String>

    /**
     * Import a private key into the application KeyStore.
     *
     * @param key The private key to be saved.
     * @param alias Alias to be assigned to the private key.
     * @param password Password used to protect the certificate.
     */
    fun importMutualTlsCertificate(
        key: ByteArray,
        alias: String,
        password: String,
    ): ImportPrivateKeyResult

    /**
     * Removes the mTLS key from storage.
     */
    fun removeMutualTlsKey(alias: String, host: MutualTlsKeyHost)
}
