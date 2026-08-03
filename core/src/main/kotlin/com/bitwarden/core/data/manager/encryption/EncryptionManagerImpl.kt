package com.bitwarden.core.data.manager.encryption

import android.security.keystore.KeyProperties
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.core.data.util.flatMap
import java.security.Key
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

private const val IV_SIZE_BYTES: Int = 12
private const val CIPHER_TRANSFORMATION: String = KeyProperties.KEY_ALGORITHM_AES + "/" +
    KeyProperties.BLOCK_MODE_GCM + "/" +
    KeyProperties.ENCRYPTION_PADDING_NONE
private const val TAG_LENGTH: Int = 128

/**
 * The default implementation of [EncryptionManager].
 */
internal class EncryptionManagerImpl(
    private val keystoreManager: KeystoreManager,
) : EncryptionManager {
    override fun decrypt(
        alias: String,
        bytes: ByteArray,
    ): Result<ByteArray> = keystoreManager
        .getKeyOrNull(alias = alias)
        .flatMap {
            it?.asSuccess() ?: IllegalStateException("Alias does not exist").asFailure()
        }
        .mapCatching {
            val iv = bytes.copyOfRange(fromIndex = 0, toIndex = IV_SIZE_BYTES)
            it.generateCipher(initCipher = InitCipher.Decrypt(iv = iv))
        }
        .mapCatching {
            val cipherText = bytes.copyOfRange(fromIndex = IV_SIZE_BYTES, toIndex = bytes.size)
            it.doFinal(cipherText)
        }

    override fun encrypt(
        alias: String,
        bytes: ByteArray,
    ): Result<ByteArray> = keystoreManager
        .getOrCreateKey(alias = alias)
        .mapCatching { it.generateCipher(initCipher = InitCipher.Encrypt) }
        .mapCatching { it.iv + it.doFinal(bytes) }
}

private sealed class InitCipher {
    data class Decrypt(val iv: ByteArray) : InitCipher() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            return iv.contentEquals(other = (other as Decrypt).iv)
        }

        override fun hashCode(): Int = iv.contentHashCode()
    }

    data object Encrypt : InitCipher()
}

private fun Key.generateCipher(
    initCipher: InitCipher,
): Cipher = Cipher
    .getInstance(CIPHER_TRANSFORMATION)
    .apply {
        when (initCipher) {
            is InitCipher.Decrypt -> {
                init(
                    Cipher.DECRYPT_MODE,
                    this@generateCipher,
                    GCMParameterSpec(TAG_LENGTH, initCipher.iv),
                )
            }

            is InitCipher.Encrypt -> init(Cipher.ENCRYPT_MODE, this@generateCipher)
        }
    }
