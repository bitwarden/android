package com.bitwarden.authenticatorbridge.util

import android.security.keystore.KeyProperties
import com.bitwarden.authenticatorbridge.model.AddTotpLoginItemData
import com.bitwarden.authenticatorbridge.model.SharedAccountData
import com.bitwarden.authenticatorbridge.model.SymmetricEncryptionKeyData
import com.bitwarden.authenticatorbridge.model.toByteArrayContainer
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator

class EncryptionUtilTest {

    @Test
    fun `generateSecretKey should return success when there are no internal exceptions`() {
        val secretKey = generateSecretKey()
        assertTrue(secretKey.isSuccess)
        assertNotNull(secretKey.getOrNull())
    }

    @Test
    fun `generateSecretKey should return failure when KeyGenerator getInstance throws`() {
        mockkStatic(KeyGenerator::class)
        every { KeyGenerator.getInstance("AES") } throws NoSuchAlgorithmException()
        val secretKey = generateSecretKey()
        assertTrue(secretKey.isFailure)
        unmockkStatic(KeyGenerator::class)
    }

    @Test
    fun `toFingerprint should return success when there are no internal exceptions`() {
        val keyData = SymmetricEncryptionKeyData(
            symmetricEncryptionKey = generateSecretKey().getOrThrow().encoded.toByteArrayContainer()
        )
        val result = keyData.toFingerprint()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `toFingerprint should return failure when MessageDigest getInstance fails`() {
        mockkStatic(MessageDigest::class)
        every { MessageDigest.getInstance("SHA-256") } throws NoSuchAlgorithmException()
        val keyData = SymmetricEncryptionKeyData(
            symmetricEncryptionKey = generateSecretKey().getOrThrow().encoded.toByteArrayContainer()
        )
        val result = keyData.toFingerprint()
        assertTrue(result.isFailure)
        unmockkStatic(MessageDigest::class)
    }

    @Test
    fun `encrypt SharedAccountData should return success when there are no internal exceptions`() {
        val result = SHARED_ACCOUNT_DATA.encrypt(SYMMETRIC_KEY)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `encrypt SharedAccountData should return failure when generateCipher fails`() {
        mockkStatic(Cipher::class)
        every {
            Cipher.getInstance(CIPHER_TRANSFORMATION)
        } throws NoSuchAlgorithmException()
        val result = SHARED_ACCOUNT_DATA.encrypt(SYMMETRIC_KEY)
        assertTrue(result.isFailure)
        unmockkStatic(Cipher::class)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `decrypt EncryptedSharedAccountData should return success when there are no internal exceptions`() {
        val result = ENCRYPTED_SHARED_ACCOUNT_DATA.decrypt(SYMMETRIC_KEY)
        assertTrue(result.isSuccess)
        assertEquals(SHARED_ACCOUNT_DATA, result.getOrThrow())
    }

    @Test
    fun `decrypt EncryptedSharedAccountData should return failure when generateCipher fails`() {
        mockkStatic(Cipher::class)
        every {
            Cipher.getInstance(CIPHER_TRANSFORMATION)
        } throws NoSuchAlgorithmException()
        val result = ENCRYPTED_SHARED_ACCOUNT_DATA.decrypt(SYMMETRIC_KEY)
        assertTrue(result.isFailure)
        unmockkStatic(Cipher::class)
    }

    @Test
    fun `encrypting and decrypting SharedAccountData should leave the data untransformed`() {
        val result = SHARED_ACCOUNT_DATA
            .encrypt(SYMMETRIC_KEY)
            .getOrThrow()
            .decrypt(SYMMETRIC_KEY)
        assertEquals(
            SHARED_ACCOUNT_DATA,
            result.getOrThrow()
        )
    }

    @Test
    @Suppress("MaxLineLength")
    fun `encrypt AddTotpLoginItemData should return success wwhen there are no internal exceptions`() {
        val result = ADD_TOTP_ITEM.encrypt(SYMMETRIC_KEY)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `encrypt AddTotpLoginItemData should return failure when generateCipher fails`() {
        mockkStatic(Cipher::class)
        every {
            Cipher.getInstance(CIPHER_TRANSFORMATION)
        } throws NoSuchAlgorithmException()
        val result = ADD_TOTP_ITEM.encrypt(SYMMETRIC_KEY)
        assertTrue(result.isFailure)
        unmockkStatic(Cipher::class)
    }

    @Test
    @Suppress("MaxLIneLength")
    fun `decrypt EncryptedAddTotpLoginItemData should return success when there are no internal exceptions`() {
        val result = ENCRYPTED_ADD_TOTP_ITEM.decrypt(SYMMETRIC_KEY)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `decrypt EncryptedAddTotpLoginItemData should return failure when generateCipher fails`() {
        mockkStatic(Cipher::class)
        every {
            Cipher.getInstance(CIPHER_TRANSFORMATION)
        } throws NoSuchAlgorithmException()
        val result = ENCRYPTED_ADD_TOTP_ITEM.decrypt(SYMMETRIC_KEY)
        assertTrue(result.isFailure)
        unmockkStatic(Cipher::class)
    }

    @Test
    fun `encrypting and decrypting AddTotpLoginItemData should leave the data untransformed`() {
        val result = ADD_TOTP_ITEM
            .encrypt(SYMMETRIC_KEY)
            .getOrThrow()
            .decrypt(SYMMETRIC_KEY)
        assertEquals(
            ADD_TOTP_ITEM,
            result.getOrThrow()
        )
    }

    @Test
    fun `toSymmetricEncryptionKeyData should wrap the given ByteArray`() {
        val sourceArray = generateSecretKey().getOrThrow().encoded
        val wrappedArray = sourceArray.toSymmetricEncryptionKeyData()
        assertTrue(sourceArray.contentEquals(wrappedArray.symmetricEncryptionKey.byteArray))
    }
}

private val SHARED_ACCOUNT_DATA = SharedAccountData(
    accounts = listOf(
        SharedAccountData.Account(
            userId = "userId",
            name = "Johnny Appleseed",
            email = "johnyapples@test.com",
            environmentLabel = "bitwarden.com",
            totpUris = listOf("test.com"),
        )
    )
)

private val ADD_TOTP_ITEM = AddTotpLoginItemData(
    totpUri = "test.com"
)

private val SYMMETRIC_KEY = SymmetricEncryptionKeyData(
    symmetricEncryptionKey = generateSecretKey().getOrThrow().encoded.toByteArrayContainer()
)

private val ENCRYPTED_SHARED_ACCOUNT_DATA =
    SHARED_ACCOUNT_DATA.encrypt(SYMMETRIC_KEY).getOrThrow()

private val ENCRYPTED_ADD_TOTP_ITEM = ADD_TOTP_ITEM.encrypt(SYMMETRIC_KEY).getOrThrow()

private const val CIPHER_TRANSFORMATION = KeyProperties.KEY_ALGORITHM_AES + "/" +
    KeyProperties.BLOCK_MODE_CBC + "/" +
    "PKCS5PADDING"
