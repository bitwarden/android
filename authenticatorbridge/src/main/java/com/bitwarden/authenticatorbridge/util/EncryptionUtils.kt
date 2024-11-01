package com.bitwarden.authenticatorbridge.util

import android.security.keystore.KeyProperties
import com.bitwarden.authenticatorbridge.IAuthenticatorBridgeService
import com.bitwarden.authenticatorbridge.model.AddTotpLoginItemData
import com.bitwarden.authenticatorbridge.model.AddTotpLoginItemDataJson
import com.bitwarden.authenticatorbridge.model.EncryptedAddTotpLoginItemData
import com.bitwarden.authenticatorbridge.model.EncryptedSharedAccountData
import com.bitwarden.authenticatorbridge.model.SharedAccountData
import com.bitwarden.authenticatorbridge.model.SharedAccountDataJson
import com.bitwarden.authenticatorbridge.model.SymmetricEncryptionKeyData
import com.bitwarden.authenticatorbridge.model.SymmetricEncryptionKeyFingerprintData
import com.bitwarden.authenticatorbridge.model.toByteArrayContainer
import kotlinx.serialization.encodeToString
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Generate a symmetric [SecretKey] that will used for encrypting IPC traffic.
 *
 * This is intended to be used for implementing
 * [IAuthenticatorBridgeService.getSymmetricEncryptionKeyData].
 */
fun generateSecretKey(): Result<SecretKey> = runCatching {
    val keygen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES)
    keygen.init(256)
    keygen.generateKey()
}

/**
 * Generate a fingerprint for the given symmetric key.
 *
 * This is intended to be used for implementing
 * [IAuthenticatorBridgeService.checkSymmetricEncryptionKeyFingerprint], which allows callers of the service
 * to verify that they have the correct symmetric key without actually having to send the key.
 */
fun SymmetricEncryptionKeyData.toFingerprint(): Result<SymmetricEncryptionKeyFingerprintData> =
    runCatching {
        val messageDigest = MessageDigest.getInstance(KeyProperties.DIGEST_SHA256)
        messageDigest.reset()
        messageDigest.update(this.symmetricEncryptionKey.byteArray)
        SymmetricEncryptionKeyFingerprintData(messageDigest.digest().toByteArrayContainer())
    }

/**
 * Encrypt [SharedAccountData].
 *
 * This is intended to be used by the main Bitwarden app during a
 * [IAuthenticatorBridgeService.syncAccounts] call.
 *
 * @param symmetricEncryptionKeyData Symmetric key used for encryption.
 */
fun SharedAccountData.encrypt(
    symmetricEncryptionKeyData: SymmetricEncryptionKeyData,
): Result<EncryptedSharedAccountData> = runCatching {
    val encodedKey = symmetricEncryptionKeyData.symmetricEncryptionKey.byteArray
    val key = encodedKey.toSecretKey()
    val cipher = generateCipher()
    cipher.init(Cipher.ENCRYPT_MODE, key)
    val jsonString = JSON.encodeToString(this.toJsonModel())
    val encryptedJsonString = cipher.doFinal(jsonString.encodeToByteArray()).toByteArrayContainer()

    EncryptedSharedAccountData(
        initializationVector = cipher.iv.toByteArrayContainer(),
        encryptedAccountsJson = encryptedJsonString,
    )
}

/**
 * Decrypt [EncryptedSharedAccountData].
 *
 * @param symmetricEncryptionKeyData Symmetric key used for decryption.
 */
internal fun EncryptedSharedAccountData.decrypt(
    symmetricEncryptionKeyData: SymmetricEncryptionKeyData,
): Result<SharedAccountData> = runCatching {
    val encodedKey = symmetricEncryptionKeyData
        .symmetricEncryptionKey
        .byteArray
    val key = encodedKey.toSecretKey()

    val iv = IvParameterSpec(this.initializationVector.byteArray)
    val cipher = generateCipher()
    cipher.init(Cipher.DECRYPT_MODE, key, iv)
    val decryptedModel = JSON.decodeFromString<SharedAccountDataJson>(
        cipher.doFinal(this.encryptedAccountsJson.byteArray).decodeToString()
    )
    decryptedModel.toDomainModel()
}

/**
 * Encrypt [AddTotpLoginItemData].
 *
 * @param symmetricEncryptionKeyData Symmetric key used for encryption.
 */
internal fun AddTotpLoginItemData.encrypt(
    symmetricEncryptionKeyData: SymmetricEncryptionKeyData,
): Result<EncryptedAddTotpLoginItemData> = runCatching {
    val encodedKey = symmetricEncryptionKeyData.symmetricEncryptionKey.byteArray
    val key = encodedKey.toSecretKey()
    val cipher = generateCipher()
    cipher.init(Cipher.ENCRYPT_MODE, key)
    val encryptedJsonString =
        cipher.doFinal(JSON.encodeToString(this.toJsonModel()).encodeToByteArray())

    EncryptedAddTotpLoginItemData(
        initializationVector = cipher.iv.toByteArrayContainer(),
        encryptedTotpUriJson = encryptedJsonString.toByteArrayContainer(),
    )
}

/**
 * Decrypt [EncryptedSharedAccountData].
 *
 * @param symmetricEncryptionKeyData Symmetric key used for decryption.
 */
fun EncryptedAddTotpLoginItemData.decrypt(
    symmetricEncryptionKeyData: SymmetricEncryptionKeyData,
): Result<AddTotpLoginItemData> = runCatching {
    val encodedKey = symmetricEncryptionKeyData
        .symmetricEncryptionKey
        .byteArray
    val key = encodedKey.toSecretKey()

    val iv = IvParameterSpec(this.initializationVector.byteArray)
    val cipher = generateCipher()
    cipher.init(Cipher.DECRYPT_MODE, key, iv)
    val decryptedModel = JSON.decodeFromString<AddTotpLoginItemDataJson>(
        cipher.doFinal(this.encryptedTotpUriJson.byteArray).decodeToString()
    )
    decryptedModel.toDomainModel()
}

/**
 * Helper function for converting a [ByteArray] to a type safe [SymmetricEncryptionKeyData].
 *
 * This is useful since callers may be storing encryption key data as a [ByteArray] under the hood
 * and must convert to a [SymmetricEncryptionKeyData] to use the SDK's encryption APIs.
 */
fun ByteArray.toSymmetricEncryptionKeyData(): SymmetricEncryptionKeyData =
    SymmetricEncryptionKeyData(toByteArrayContainer())

/**
 * Convert the given [ByteArray] to a [SecretKey].
 */
private fun ByteArray.toSecretKey(): SecretKey =
    SecretKeySpec(this, 0, this.size, KeyProperties.KEY_ALGORITHM_AES)

/**
 * Helper function for generating a [Cipher] that can be used for encrypting/decrypting using
 * [SymmetricEncryptionKeyData].
 */
private fun generateCipher(): Cipher =
    Cipher.getInstance(
        KeyProperties.KEY_ALGORITHM_AES + "/" +
            KeyProperties.BLOCK_MODE_CBC + "/" +
            "PKCS5PADDING"
    )

/**
 * Helper function for converting [SharedAccountData] to a serializable [SharedAccountDataJson].
 */
private fun SharedAccountData.toJsonModel() = SharedAccountDataJson(
    accounts = this.accounts.map { account ->
        SharedAccountDataJson.AccountJson(
            userId = account.userId,
            name = account.name,
            environmentLabel = account.environmentLabel,
            email = account.email,
            totpUris = account.totpUris,
        )
    }
)

/**
 * Helper function for converting [SharedAccountDataJson] to a [SharedAccountData].
 */
private fun SharedAccountDataJson.toDomainModel() = SharedAccountData(
    accounts = this.accounts.map { account ->
        SharedAccountData.Account(
            userId = account.userId,
            name = account.name,
            environmentLabel = account.environmentLabel,
            email = account.email,
            totpUris = account.totpUris,
        )
    }
)

/**
 * Helper function for converting [AddTotpLoginItemDataJson] to a [AddTotpLoginItemData].
 */
private fun AddTotpLoginItemDataJson.toDomainModel() = AddTotpLoginItemData(
    totpUri = totpUri,
)

/**
 * Helper function for converting [AddTotpLoginItemData] to a serializable [AddTotpLoginItemDataJson].
 */
private fun AddTotpLoginItemData.toJsonModel() = AddTotpLoginItemDataJson(
    totpUri = totpUri,
)
