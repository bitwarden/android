package com.bitwarden.core.data.manager.encryption

import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class EncryptionManagerTest {

    private val mockSecretKey = mockk<SecretKey>()
    private val mockKeystoreManager = mockk<KeystoreManager> {
        every { getOrCreateKey(alias = ALIAS) } returns mockSecretKey.asSuccess()
        every { getKeyOrNull(alias = ALIAS) } returns mockSecretKey.asSuccess()
    }

    // Real Cipher code cannot run in a JVM unit test (the "NoPadding" transformation is only
    // registered on Android and MockK cannot call originals on the JPMS-protected
    // javax.crypto.Cipher), so the cipher is fully mocked with a symmetric byte transformation
    // that produces non-UTF-8 bytes, just like real ciphertext.
    private val mockCipher = mockk<Cipher> {
        every { init(Cipher.ENCRYPT_MODE, mockSecretKey) } just runs
        every { init(Cipher.DECRYPT_MODE, mockSecretKey, any<GCMParameterSpec>()) } just runs
        every { iv } returns IV
        every { doFinal(any()) } answers { firstArg<ByteArray>().toggleBits() }
    }

    private val encryptionManager: EncryptionManager = EncryptionManagerImpl(
        keystoreManager = mockKeystoreManager,
    )

    @BeforeEach
    fun setUp() {
        mockkStatic(Cipher::class)
        every { Cipher.getInstance("AES/GCM/NoPadding") } returns mockCipher
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Cipher::class)
    }

    @Test
    fun `encrypt should return the IV followed by the ciphertext`() {
        val result = encryptionManager.encrypt(alias = ALIAS, bytes = PLAINTEXT_BYTES)

        val encryptedBytes = result.getOrThrow()
        assertArrayEquals(IV, encryptedBytes.copyOfRange(fromIndex = 0, toIndex = IV.size))
        assertArrayEquals(
            PLAINTEXT_BYTES.toggleBits(),
            encryptedBytes.copyOfRange(fromIndex = IV.size, toIndex = encryptedBytes.size),
        )
        verify(exactly = 1) {
            mockKeystoreManager.getOrCreateKey(alias = ALIAS)
            mockCipher.init(Cipher.ENCRYPT_MODE, mockSecretKey)
        }
    }

    @Test
    fun `encrypt should return failure when the key cannot be retrieved or created`() {
        val error = Throwable()
        every { mockKeystoreManager.getOrCreateKey(alias = ALIAS) } returns error.asFailure()

        assertEquals(
            error.asFailure(),
            encryptionManager.encrypt(alias = ALIAS, bytes = PLAINTEXT_BYTES),
        )
        verify(exactly = 0) {
            Cipher.getInstance(any())
        }
    }

    @Test
    fun `decrypt should strip the IV and return the decrypted bytes`() {
        val cipherText = PLAINTEXT_BYTES.toggleBits()

        val result = encryptionManager.decrypt(alias = ALIAS, bytes = IV + cipherText)

        assertArrayEquals(PLAINTEXT_BYTES, result.getOrThrow())
        verify(exactly = 1) {
            mockCipher.init(
                Cipher.DECRYPT_MODE,
                mockSecretKey,
                match<GCMParameterSpec> { it.iv.contentEquals(IV) },
            )
            mockCipher.doFinal(match { it.contentEquals(cipherText) })
        }
    }

    @Test
    fun `decrypt should return failure when no key exists`() {
        every { mockKeystoreManager.getKeyOrNull(alias = ALIAS) } returns null.asSuccess()

        val result = encryptionManager.decrypt(alias = ALIAS, bytes = IV + PLAINTEXT_BYTES)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `decrypt should return failure when the key cannot be retrieved`() {
        val error = Throwable()
        every { mockKeystoreManager.getKeyOrNull(alias = ALIAS) } returns error.asFailure()

        assertEquals(
            error.asFailure(),
            encryptionManager.decrypt(alias = ALIAS, bytes = IV + PLAINTEXT_BYTES),
        )
    }

    @Test
    fun `decrypt should recover the exact bytes passed to encrypt`() {
        val encryptedBytes = encryptionManager
            .encrypt(alias = ALIAS, bytes = PLAINTEXT_BYTES)
            .getOrThrow()

        assertArrayEquals(
            PLAINTEXT_BYTES,
            encryptionManager.decrypt(alias = ALIAS, bytes = encryptedBytes).getOrThrow(),
        )
    }
}

/**
 * A self-inverting stand-in for encryption that, like real ciphertext, produces bytes that are
 * not valid UTF-8.
 */
private fun ByteArray.toggleBits(): ByteArray = this
    .map { (it.toInt() xor 0xAA).toByte() }
    .toByteArray()

private const val ALIAS: String = "mockAlias"

private val PLAINTEXT_BYTES: ByteArray = "mockValue with unicode 🔐".encodeToByteArray()

// 0x80–0x8F are UTF-8 continuation bytes, which are invalid as leading bytes and would be
// destroyed by a UTF-8 decode/encode round trip.
private val IV: ByteArray = ByteArray(12) { (0x80 + it).toByte() }
