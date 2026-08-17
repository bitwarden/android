package com.bitwarden.core.data.manager.encryption

/**
 * A manager responsible for encrypting and decrypting data.
 */
interface EncryptionManager {
    /**
     * Decrypts the string with the Secret associated with the [alias].
     */
    fun decrypt(alias: String, bytes: ByteArray): Result<ByteArray>

    /**
     * Encrypts the string with the Secret associated with the [alias].
     */
    fun encrypt(alias: String, bytes: ByteArray): Result<ByteArray>
}
