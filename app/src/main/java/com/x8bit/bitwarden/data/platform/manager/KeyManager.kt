package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.platform.manager.model.ImportPrivateKeyResult
import com.x8bit.bitwarden.data.platform.datasource.disk.model.MutualTlsCertificate
import com.x8bit.bitwarden.data.platform.datasource.disk.model.MutualTlsKeyHost

/**
 * Primary access point for disk information related to key data.
 */
interface KeyManager {

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

    /**
     * Retrieve the certificate chain for the selected mTLS key.
     */
    fun getMutualTlsCertificateChain(
        alias: String,
        host: MutualTlsKeyHost,
    ): MutualTlsCertificate?
}
