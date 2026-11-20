package com.x8bit.bitwarden.data.platform.manager

import androidx.annotation.WorkerThread
import com.bitwarden.network.ssl.CertificateProvider
import com.x8bit.bitwarden.data.platform.datasource.disk.model.MutualTlsCertificate
import com.x8bit.bitwarden.data.platform.datasource.disk.model.MutualTlsKeyHost
import com.x8bit.bitwarden.data.platform.manager.model.ImportPrivateKeyResult

/**
 * Primary access point for disk information related to key data.
 */
interface CertificateManager : CertificateProvider {

    /**
     * The mTLS certificate configured for the current environment, resolved from its key store on
     * every access, or `null` when none is configured or it cannot be loaded.
     */
    @get:WorkerThread
    val mutualTlsCertificate: MutualTlsCertificate?

    /**
     * Resolves the mTLS certificate that [keyUri] references from its key store, or returns `null`
     * when [keyUri] is malformed or the certificate cannot be loaded.
     */
    @WorkerThread
    fun getMutualTlsCertificate(keyUri: String): MutualTlsCertificate?

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
