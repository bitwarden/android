package com.bitwarden.core.data.manager.encryption

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.bitwarden.core.data.manager.BuildInfoManager
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.core.data.util.flatMap
import timber.log.Timber
import java.security.InvalidAlgorithmParameterException
import java.security.Key
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.ProviderException
import java.security.UnrecoverableKeyException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

private const val KEYSTORE_TYPE_ANDROID: String = "AndroidKeyStore"
private const val KEY_SIZE: Int = 256

/**
 * Default implementation of [KeystoreManager].
 */
internal class KeystoreManagerImpl(
    private val buildInfoManager: BuildInfoManager,
) : KeystoreManager {

    private val androidKeystore: KeyStore by lazy {
        KeyStore
            .getInstance(KEYSTORE_TYPE_ANDROID)
            .also { it.load(null) }
    }

    override fun getOrCreateKey(
        alias: String,
    ): Result<Key> = this
        .getKeyOrNull(alias = alias)
        .flatMap {
            // If the key is null, then it did not exist and we should generate one.
            // If an error occurred, then we should just send back the error.
            it?.asSuccess() ?: generateKeyOrNull(alias = alias)
        }

    override fun getKeyOrNull(alias: String): Result<Key?> =
        try {
            androidKeystore.getKey(alias.formatAlias(), null).asSuccess()
        } catch (e: KeyStoreException) {
            // Keystore was not loaded
            Timber.w(e, "getKey failed to retrieve secret key")
            e.asFailure()
        } catch (e: NoSuchAlgorithmException) {
            // Keystore algorithm cannot be found
            Timber.w(e, "getKey failed to retrieve secret key")
            e.asFailure()
        } catch (e: UnrecoverableKeyException) {
            // Key could not be recovered
            Timber.w(e, "getKey failed to retrieve secret key")
            e.asFailure()
        }

    override fun hasKey(
        alias: String,
    ): Boolean = androidKeystore.containsAlias(alias.formatAlias())

    override fun removeKey(alias: String): Boolean =
        try {
            androidKeystore.deleteEntry(alias.formatAlias())
            true
        } catch (e: KeyStoreException) {
            Timber.e(e, "removeKey failed to delete keystore entry")
            false
        }

    /**
     * Generates and stores a new AES [SecretKey] under [alias], or `null` if a key cannot be
     * generated.
     */
    private fun generateKeyOrNull(alias: String): Result<Key> {
        val keyGen = try {
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_TYPE_ANDROID)
        } catch (e: NoSuchAlgorithmException) {
            Timber.w(e, "generateKeyOrNull failed to get key generator instance")
            return e.asFailure()
        } catch (e: NoSuchProviderException) {
            Timber.w(e, "generateKeyOrNull failed to get key generator instance")
            return e.asFailure()
        } catch (e: IllegalArgumentException) {
            Timber.w(e, "generateKeyOrNull failed to get key generator instance")
            return e.asFailure()
        }

        return try {
            keyGen.init(getKeyGenParameterSpec(alias = alias))
            keyGen.generateKey().asSuccess()
        } catch (e: InvalidAlgorithmParameterException) {
            Timber.w(e, "generateKeyOrNull failed to initialize and generate key")
            e.asFailure()
        } catch (e: ProviderException) {
            Timber.w(e, "generateKeyOrNull failed to initialize and generate key")
            e.asFailure()
        }
    }

    private fun getKeyGenParameterSpec(
        alias: String,
    ): KeyGenParameterSpec =
        KeyGenParameterSpec
            .Builder(
                alias.formatAlias(),
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE)
            .build()

    private fun String.formatAlias(): String = "${buildInfoManager.applicationId}.$this"
}
