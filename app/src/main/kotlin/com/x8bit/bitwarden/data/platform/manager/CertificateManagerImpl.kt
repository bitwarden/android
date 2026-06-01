package com.x8bit.bitwarden.data.platform.manager

import android.content.Context
import android.net.Uri
import android.security.KeyChain
import android.security.KeyChainException
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.core.net.toUri
import com.bitwarden.core.data.repository.error.MissingPropertyException
import com.x8bit.bitwarden.data.platform.datasource.disk.model.MutualTlsCertificate
import com.x8bit.bitwarden.data.platform.datasource.disk.model.MutualTlsKeyHost
import com.x8bit.bitwarden.data.platform.manager.model.ImportPrivateKeyResult
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import timber.log.Timber
import java.io.IOException
import java.net.Socket
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.Principal
import java.security.PrivateKey
import java.security.UnrecoverableKeyException
import java.security.cert.Certificate
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

/**
 * Default implementation of [CertificateManager].
 */
@Suppress("TooManyFunctions")
class CertificateManagerImpl(
    private val context: Context,
    private val environmentRepository: EnvironmentRepository,
) : CertificateManager {

    /*
        This property must only be accessed from a background thread. Accessing this property from
        the main thread will result in an exception being thrown when retrieving the mutual TLS
        certificate from [KeyManager].
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    @get:WorkerThread
    internal val mutualTlsCertificate: MutualTlsCertificate?
        get() {
            val keyUri = getKeyUri()
                ?: return null

            val host = MutualTlsKeyHost
                .entries
                .find { it.name == keyUri.authority }
                ?: return null

            val alias = keyUri.path
                ?.trim('/')
                ?.takeUnless { it.isEmpty() }
                ?: return null

            return getMutualTlsCertificateChain(
                alias = alias,
                host = host,
            )
        }

    override fun getMutualTlsKeyAliases(): List<String> = androidKeyStore.aliases().toList()

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    @WorkerThread
    override fun importMutualTlsCertificate(
        key: ByteArray,
        alias: String,
        password: String,
    ): ImportPrivateKeyResult {
        // Step 1: Load PKCS12 bytes into a KeyStore.
        val pkcs12KeyStore: KeyStore = key
            .inputStream()
            .use { stream ->
                try {
                    KeyStore
                        .getInstance(KEYSTORE_TYPE_PKCS12)
                        .also { it.load(stream, password.toCharArray()) }
                } catch (e: KeyStoreException) {
                    Timber.e(e, "Failed to load PKCS12 bytes")
                    return ImportPrivateKeyResult.Error.UnsupportedKey(throwable = e)
                } catch (e: IOException) {
                    Timber.e(e, "Format or password error while loading PKCS12 bytes")
                    return when (e.cause) {
                        is UnrecoverableKeyException -> {
                            ImportPrivateKeyResult.Error.UnrecoverableKey(throwable = e)
                        }

                        else -> {
                            ImportPrivateKeyResult.Error.KeyStoreOperationFailed(throwable = e)
                        }
                    }
                } catch (e: CertificateException) {
                    Timber.e(e, "Unable to load certificate chain")
                    return ImportPrivateKeyResult.Error.InvalidCertificateChain(throwable = e)
                } catch (e: NoSuchAlgorithmException) {
                    Timber.e(e, "Cryptographic algorithm not supported")
                    return ImportPrivateKeyResult.Error.UnsupportedKey(throwable = e)
                }
            }

        // Step 2: Get a list of aliases and choose the first one.
        val internalAlias = pkcs12KeyStore.aliases()
            ?.takeIf { it.hasMoreElements() }
            ?.nextElement()
            ?: return ImportPrivateKeyResult.Error.UnsupportedKey(
                throwable = MissingPropertyException("Internal Alias"),
            )

        // Step 3: Extract PrivateKey and X.509 certificate from the KeyStore and verify
        // certificate alias.
        val privateKey = try {
            pkcs12KeyStore
                .getKey(internalAlias, password.toCharArray())
                ?: return ImportPrivateKeyResult.Error.UnrecoverableKey(
                    throwable = MissingPropertyException("Private Key"),
                )
        } catch (e: UnrecoverableKeyException) {
            Timber.e(e, "Failed to get private key")
            return ImportPrivateKeyResult.Error.UnrecoverableKey(throwable = e)
        }

        val certChain: Array<Certificate> = pkcs12KeyStore
            .getCertificateChain(internalAlias)
            ?.takeUnless { it.isEmpty() }
            ?: return ImportPrivateKeyResult.Error.InvalidCertificateChain(
                throwable = MissingPropertyException("Certificate Chain"),
            )

        // Step 4: Store the private key and X.509 certificate in the AndroidKeyStore if the alias
        // does not exists.
        with(androidKeyStore) {
            try {
                setKeyEntry(alias, privateKey, null, certChain)
            } catch (e: KeyStoreException) {
                Timber.e(e, "Failed to import key into Android KeyStore")
                return ImportPrivateKeyResult.Error.KeyStoreOperationFailed(throwable = e)
            }
        }
        return ImportPrivateKeyResult.Success(alias)
    }

    override fun removeMutualTlsKey(
        alias: String,
        host: MutualTlsKeyHost,
    ) {
        when (host) {
            MutualTlsKeyHost.ANDROID_KEY_STORE -> removeKeyFromAndroidKeyStore(alias)
            else -> Unit
        }
    }

    @WorkerThread
    override fun chooseClientAlias(
        keyType: Array<out String>?,
        issuers: Array<out Principal>?,
        socket: Socket?,
    ): String = mutualTlsCertificate?.alias.orEmpty()

    @WorkerThread
    override fun getCertificateChain(
        alias: String?,
    ): Array<X509Certificate>? = mutualTlsCertificate?.certificateChain?.toTypedArray()

    @WorkerThread
    override fun getPrivateKey(alias: String?): PrivateKey? = mutualTlsCertificate?.privateKey

    private fun getKeyUri(): Uri? = environmentRepository
        .environment
        .environmentUrlData
        .keyUri
        ?.toUri()

    private fun removeKeyFromAndroidKeyStore(alias: String) {
        try {
            androidKeyStore.deleteEntry(alias)
        } catch (e: KeyStoreException) {
            Timber.e(e, "Failed to remove key from Android KeyStore")
        }
    }

    private fun getMutualTlsCertificateChain(
        alias: String,
        host: MutualTlsKeyHost,
    ): MutualTlsCertificate? = when (host) {
        MutualTlsKeyHost.ANDROID_KEY_STORE -> getKeyFromAndroidKeyStore(alias)

        MutualTlsKeyHost.KEY_CHAIN -> getSystemKeySpecOrNull(alias)
    }

    private fun getSystemKeySpecOrNull(alias: String): MutualTlsCertificate? {
        val systemPrivateKey = try {
            KeyChain.getPrivateKey(context, alias)
        } catch (e: KeyChainException) {
            Timber.e(e, "Requested alias not found in system KeyChain")
            null
        }
            ?: return null

        val systemCertificateChain = try {
            KeyChain.getCertificateChain(context, alias)
        } catch (e: KeyChainException) {
            Timber.e(e, "Unable to access certificate chain for provided alias")
            null
        }
            ?: return null

        return MutualTlsCertificate(
            alias = alias,
            certificateChain = systemCertificateChain.toList(),
            privateKey = systemPrivateKey,
        )
    }

    private fun getKeyFromAndroidKeyStore(alias: String): MutualTlsCertificate? =
        with(androidKeyStore) {
            try {
                val privateKeyRef = (getKey(alias, null) as? PrivateKey)
                    ?: return null
                val certChain = getCertificateChain(alias)
                    .mapNotNull { it as? X509Certificate }
                    .takeUnless { it.isEmpty() }
                    ?: return null
                MutualTlsCertificate(
                    alias = alias,
                    certificateChain = certChain,
                    privateKey = privateKeyRef,
                )
            } catch (e: KeyStoreException) {
                Timber.e(e, "Failed to load Android KeyStore")
                null
            } catch (e: UnrecoverableKeyException) {
                Timber.e(e, "Failed to load client certificate from Android KeyStore")
                null
            } catch (e: NoSuchAlgorithmException) {
                Timber.e(e, "Key cannot be recovered. Password may be incorrect.")
                null
            }
        }

    private val androidKeyStore
        get() = KeyStore
            .getInstance(KEYSTORE_TYPE_ANDROID)
            .also { it.load(null) }
}

private const val KEYSTORE_TYPE_ANDROID = "AndroidKeyStore"
private const val KEYSTORE_TYPE_PKCS12 = "pkcs12"
