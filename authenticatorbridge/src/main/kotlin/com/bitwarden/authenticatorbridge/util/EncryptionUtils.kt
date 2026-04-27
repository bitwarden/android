@file:Suppress("TooManyFunctions")

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
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Generate a symmetric [SecretKey] that will used for encrypting IPC traffic.
 *
 * This is intended to be used for implementing
 * [IAuthenticatorBridgeService.getSymmetricEncryptionKeyData].
 */
@Suppress("MagicNumber")
fun generateSecretKey(): Result<SecretKey> = runCatching {
    val keygen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES)
    keygen.init(256)
    keygen.generateKey()
}

/**
 * Generate a fingerprint for the given symmetric key.
 *
 * This is intended to be used for implementing
 * [IAuthenticatorBridgeService.checkSymmetricEncryptionKeyFingerprint], which allows callers of the
 * service to verify that they have the correct symmetric key without actually having to send the
 * key.
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
        cipher.doFinal(this.encryptedAccountsJson.byteArray).decodeToString(),
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
        cipher.doFinal(this.encryptedTotpUriJson.byteArray).decodeToString(),
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
            "PKCS5PADDING",
    )

/**
 * Helper function for converting [SharedAccountData] to a serializable [SharedAccountDataJson].
 */
private fun SharedAccountData.toJsonModel(): SharedAccountDataJson = SharedAccountDataJson(
    accounts = this.accounts.map { account ->
        SharedAccountDataJson.AccountJson(
            userId = account.userId,
            name = account.name,
            environmentLabel = account.environmentLabel,
            email = account.email,
            // TODO: PM-34085 Remove totpUris from this model.
            totpUris = account.cipherData.mapNotNull { it.legacyUri },
            cipherData = account.cipherData.map { it.toJsonModel() },
        )
    },
)

/**
 * Helper function for converting [SharedAccountData.CipherData] to a
 * [SharedAccountDataJson.CipherJson].
 */
private fun SharedAccountData.CipherData.toJsonModel(): SharedAccountDataJson.CipherJson =
    SharedAccountDataJson.CipherJson(
        uri = this.uri,
        id = this.id,
        name = this.name,
        username = this.username,
        isFavorite = this.isFavorite,
    )

/**
 * Helper function for converting [SharedAccountDataJson] to a [SharedAccountData].
 */
private fun SharedAccountDataJson.toDomainModel(): SharedAccountData = SharedAccountData(
    accounts = this.accounts.map { account ->
        SharedAccountData.Account(
            userId = account.userId,
            name = account.name,
            environmentLabel = account.environmentLabel,
            email = account.email,
            cipherData = account.cipherData?.map { it.toCipherData() }
            // TODO: PM-34085 Remove this mapping from totpUris.
                ?: account.totpUris.map { it.toCipherData() },
        )
    },
)

/**
 * Helper function for converting [SharedAccountDataJson.CipherJson] to a
 * [SharedAccountData.CipherData].
 */
private fun SharedAccountDataJson.CipherJson.toCipherData(): SharedAccountData.CipherData =
    SharedAccountData.CipherData(
        uri = this.uri,
        legacyUri = this.uri,
        id = this.id,
        name = this.name,
        username = this.username,
        isFavorite = this.isFavorite,
    )

/**
 * Helper function for converting [String] URI to a [SharedAccountData.CipherData].
 * TODO: PM-34085 Remove this function, it is only needed for legacy support.
 */
@OptIn(ExperimentalUuidApi::class)
private fun String.toCipherData(): SharedAccountData.CipherData = SharedAccountData.CipherData(
    uri = this,
    legacyUri = this,
    id = Uuid.random().toString(),
    name = "",
    username = null,
    isFavorite = false,
)

/**
 * Helper function for converting [AddTotpLoginItemDataJson] to a [AddTotpLoginItemData].
 */
private fun AddTotpLoginItemDataJson.toDomainModel(): AddTotpLoginItemData = AddTotpLoginItemData(
    totpUri = totpUri,
)

/**
 * Helper function for converting [AddTotpLoginItemData] to a serializable
 * [AddTotpLoginItemDataJson].
 */
private fun AddTotpLoginItemData.toJsonModel(): AddTotpLoginItemDataJson = AddTotpLoginItemDataJson(
    totpUri = totpUri,
)
