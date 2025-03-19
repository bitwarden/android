package com.x8bit.bitwarden.data.platform.manager

import android.content.Context
import android.security.KeyChain
import android.security.KeyChainException
import com.x8bit.bitwarden.data.platform.datasource.disk.model.MutualTlsCertificate
import com.x8bit.bitwarden.data.platform.datasource.disk.model.MutualTlsKeyHost
import com.x8bit.bitwarden.data.platform.error.MissingPropertyException
import com.x8bit.bitwarden.data.platform.manager.model.ImportPrivateKeyResult
import timber.log.Timber
import java.io.IOException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.UnrecoverableKeyException
import java.security.cert.Certificate
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

/**
 * Default implementation of [KeyManager].
 */
class KeyManagerImpl(
    private val context: Context,
) : KeyManager {

    @Suppress("LongMethod", "CyclomaticComplexMethod")
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
                    Timber.Forest.e(e, "Failed to load PKCS12 bytes")
                    return ImportPrivateKeyResult.Error.UnsupportedKey(throwable = e)
                } catch (e: IOException) {
                    Timber.Forest.e(e, "Format or password error while loading PKCS12 bytes")
                    return when (e.cause) {
                        is UnrecoverableKeyException -> {
                            ImportPrivateKeyResult.Error.UnrecoverableKey(throwable = e)
                        }

                        else -> {
                            ImportPrivateKeyResult.Error.KeyStoreOperationFailed(throwable = e)
                        }
                    }
                } catch (e: CertificateException) {
                    Timber.Forest.e(e, "Unable to load certificate chain")
                    return ImportPrivateKeyResult.Error.InvalidCertificateChain(throwable = e)
                } catch (e: NoSuchAlgorithmException) {
                    Timber.Forest.e(e, "Cryptographic algorithm not supported")
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
            Timber.Forest.e(e, "Failed to get private key")
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
            if (containsAlias(alias)) {
                return ImportPrivateKeyResult.Error.DuplicateAlias
            }

            try {
                setKeyEntry(alias, privateKey, null, certChain)
            } catch (e: KeyStoreException) {
                Timber.Forest.e(e, "Failed to import key into Android KeyStore")
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

    override fun getMutualTlsCertificateChain(
        alias: String,
        host: MutualTlsKeyHost,
    ): MutualTlsCertificate? = when (host) {
        MutualTlsKeyHost.ANDROID_KEY_STORE -> getKeyFromAndroidKeyStore(alias)

        MutualTlsKeyHost.KEY_CHAIN -> getSystemKeySpecOrNull(alias)
    }

    private fun removeKeyFromAndroidKeyStore(alias: String) {
        try {
            androidKeyStore.deleteEntry(alias)
        } catch (e: KeyStoreException) {
            Timber.Forest.e(e, "Failed to remove key from Android KeyStore")
        }
    }

    private fun getSystemKeySpecOrNull(alias: String): MutualTlsCertificate? {
        val systemPrivateKey = try {
            KeyChain.getPrivateKey(context, alias)
        } catch (e: KeyChainException) {
            Timber.Forest.e(e, "Requested alias not found in system KeyChain")
            null
        }
            ?: return null

        val systemCertificateChain = try {
            KeyChain.getCertificateChain(context, alias)
        } catch (e: KeyChainException) {
            Timber.Forest.e(e, "Unable to access certificate chain for provided alias")
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
                Timber.Forest.e(e, "Failed to load Android KeyStore")
                null
            } catch (e: UnrecoverableKeyException) {
                Timber.Forest.e(e, "Failed to load client certificate from Android KeyStore")
                null
            } catch (e: NoSuchAlgorithmException) {
                Timber.Forest.e(e, "Key cannot be recovered. Password may be incorrect.")
                null
            } catch (e: NoSuchAlgorithmException) {
                Timber.Forest.e(e, "Algorithm not supported")
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
