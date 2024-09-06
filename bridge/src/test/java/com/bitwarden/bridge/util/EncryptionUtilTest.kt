package com.bitwarden.bridge.util

import com.bitwarden.bridge.model.AddTotpLoginItemData
import com.bitwarden.bridge.model.SharedAccountData
import com.bitwarden.bridge.model.SymmetricEncryptionKeyData
import com.bitwarden.bridge.model.toByteArrayContainer
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.KeyGenerator

class EncryptionUtilTest {

    private val TEST_SHARED_ACCOUNT_DATA = SharedAccountData(
        accounts = listOf(
            SharedAccountData.Account(
                userId = "userId",
                name = "Johnny Appleseed",
                email = "johnyapples@test.com",
                environmentLabel = "bitwarden.com",
                totpUris = listOf("test.com"),
                lastSyncTime = Instant.parse("2024-09-10T10:15:30.00Z")
            )
        )
    )

    private val TEST_ADD_TOTP_ITEM = AddTotpLoginItemData(
        totpUri = "test.com"
    )

    private val TEST_SYMMETRIC_KEY = SymmetricEncryptionKeyData(
        symmetricEncryptionKey = generateSecretKey().getOrThrow().encoded.toByteArrayContainer()
    )

    private val TEST_ENCRYPTED_SHARED_ACCOUNT_DATA =
        TEST_SHARED_ACCOUNT_DATA.encrypt(TEST_SYMMETRIC_KEY).getOrThrow()

    private val TEST_ENCRYPTED_ADD_TOTP_ITEM = TEST_ADD_TOTP_ITEM.encrypt(TEST_SYMMETRIC_KEY).getOrThrow()

    @Test
    fun `generateSecretKey should return success`() {
        val secretKey = generateSecretKey()
        assert(secretKey.isSuccess)
        assertNotNull(secretKey.getOrNull())
    }

    @Test
    fun `when KeyGenerator getInstance throws, generateSecretKey should return failure`() {
        mockkStatic(KeyGenerator::class)
        every { KeyGenerator.getInstance("AES") } throws NoSuchAlgorithmException()
        val secretKey = generateSecretKey()
        assert(secretKey.isFailure)
        unmockkStatic(KeyGenerator::class)
    }

    @Test
    fun `toFingerprint should return success`() {
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
    fun `encrypt SharedAccountData should return success`() {
        val result = TEST_SHARED_ACCOUNT_DATA.encrypt(TEST_SYMMETRIC_KEY)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `encrypt SharedAccountData should return failure when generateCipher fails`() {
        mockkStatic(Cipher::class)
        every {
            Cipher.getInstance("AES/CBC/PKCS5PADDING")
        } throws NoSuchAlgorithmException()
        val result = TEST_SHARED_ACCOUNT_DATA.encrypt(TEST_SYMMETRIC_KEY)
        assertTrue(result.isFailure)
        unmockkStatic(Cipher::class)
    }

    @Test
    fun `decrypt EncryptedSharedAccountData should return success`() {
        val result = TEST_ENCRYPTED_SHARED_ACCOUNT_DATA.decrypt(TEST_SYMMETRIC_KEY)
        assertTrue(result.isSuccess)
        assertEquals(TEST_SHARED_ACCOUNT_DATA, result.getOrThrow())
    }

    @Test
    fun `decrypt EncryptedSharedAccountData should return failure when generateCipher fails`() {
        mockkStatic(Cipher::class)
        every {
            Cipher.getInstance("AES/CBC/PKCS5PADDING")
        } throws NoSuchAlgorithmException()
        val result = TEST_ENCRYPTED_SHARED_ACCOUNT_DATA.decrypt(TEST_SYMMETRIC_KEY)
        assertTrue(result.isFailure)
        unmockkStatic(Cipher::class)
    }

    @Test
    fun `encrypting and decrypting SharedAccountData should leave the data untransformed`() {
        val result = TEST_SHARED_ACCOUNT_DATA
            .encrypt(TEST_SYMMETRIC_KEY)
            .getOrThrow()
            .decrypt(TEST_SYMMETRIC_KEY)
        assertEquals(
            TEST_SHARED_ACCOUNT_DATA,
            result.getOrThrow()
        )
    }

    @Test
    fun `encrypt AddTotpLoginItemData should return success`() {
        val result = TEST_ADD_TOTP_ITEM.encrypt(TEST_SYMMETRIC_KEY)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `encrypt AddTotpLoginItemData should return failure when generateCipher fails`() {
        mockkStatic(Cipher::class)
        every {
            Cipher.getInstance("AES/CBC/PKCS5PADDING")
        } throws NoSuchAlgorithmException()
        val result = TEST_ADD_TOTP_ITEM.encrypt(TEST_SYMMETRIC_KEY)
        assertTrue(result.isFailure)
        unmockkStatic(Cipher::class)
    }

    @Test
    fun `decrypt EncryptedAddTotpLoginItemData should return success`() {
        val result = TEST_ENCRYPTED_ADD_TOTP_ITEM.decrypt(TEST_SYMMETRIC_KEY)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `decrypt EncryptedAddTotpLoginItemData should return failure when generateCipher fails`() {
        mockkStatic(Cipher::class)
        every {
            Cipher.getInstance("AES/CBC/PKCS5PADDING")
        } throws NoSuchAlgorithmException()
        val result = TEST_ENCRYPTED_ADD_TOTP_ITEM.decrypt(TEST_SYMMETRIC_KEY)
        assertTrue(result.isFailure)
        unmockkStatic(Cipher::class)
    }

    @Test
    fun `encrypting and decrypting AddTotpLoginItemData should leave the data untransformed`() {
        val result = TEST_ADD_TOTP_ITEM
            .encrypt(TEST_SYMMETRIC_KEY)
            .getOrThrow()
            .decrypt(TEST_SYMMETRIC_KEY)
        assertEquals(
            TEST_ADD_TOTP_ITEM,
            result.getOrThrow()
        )
    }

}
