package com.bitwarden.authenticator.data.platform.manager.crypto

import com.bitwarden.authenticator.data.authenticator.manager.model.EncryptedExportJsonData
import com.bitwarden.authenticator.data.platform.manager.crypto.model.DecryptExportResult
import com.bitwarden.authenticator.data.platform.manager.crypto.model.EncryptExportResult
import com.bitwarden.core.data.manager.UuidManager
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.SecureRandom

/**
 * The fixed envelopes in `src/test/resources/exports` were produced by an implementation built
 * independently of this one, against the SDK's own test vectors for HKDF stretching and
 * AES-256-CBC-HMAC-SHA256. They are what keeps this class compatible with the other clients.
 */
class ExportEncryptionManagerTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val uuidManager: UuidManager = mockk {
        every { generateUuid() } returns VALIDATION_UUID
    }

    private val plaintextExport = readResource("password_protected_export_plaintext.json")
    private val fastKdfEnvelope = readResource("password_protected_export_fast_kdf.json")
    private val defaultKdfEnvelope = readResource("password_protected_export_default_kdf.json")

    @Test
    fun `isPasswordProtected returns true for a password protected export`() {
        assertTrue(createManager().isPasswordProtected(json = fastKdfEnvelope))
    }

    @Test
    fun `isPasswordProtected returns false for a plaintext export`() {
        assertFalse(createManager().isPasswordProtected(json = plaintextExport))
    }

    @Test
    fun `isPasswordProtected returns false for content that is not an export`() {
        assertFalse(createManager().isPasswordProtected(json = "not json at all"))
    }

    @Test
    fun `decrypt returns the plaintext export when given the correct password`() = runTest {
        assertEquals(
            DecryptExportResult.Success(json = plaintextExport),
            createManager().decrypt(json = fastKdfEnvelope, password = PASSWORD),
        )
    }

    @Test
    fun `decrypt returns IncorrectPassword when given the wrong password`() = runTest {
        assertEquals(
            DecryptExportResult.IncorrectPassword,
            createManager().decrypt(json = fastKdfEnvelope, password = "not the password"),
        )
    }

    @Test
    fun `decrypt returns Error when the encrypted data has been tampered with`() = runTest {
        val envelope = fastKdfEnvelope.toEnvelope()
        val parts = envelope.data.split(ENC_STRING_SEPARATOR).toMutableList()
        val ciphertext = parts[CIPHERTEXT_INDEX]
        parts[CIPHERTEXT_INDEX] = ciphertext.replaceRange(
            startIndex = 0,
            endIndex = 1,
            replacement = if (ciphertext.first() == 'A') "B" else "A",
        )

        assertEquals(
            DecryptExportResult.Error,
            createManager().decrypt(
                json = envelope
                    .copy(data = parts.joinToString(ENC_STRING_SEPARATOR))
                    .toJson(),
                password = PASSWORD,
            ),
        )
    }

    @Test
    fun `decrypt returns UnsupportedKdf for an argon2id export`() = runTest {
        assertEquals(
            DecryptExportResult.UnsupportedKdf,
            createManager().decrypt(
                json = fastKdfEnvelope
                    .toEnvelope()
                    .copy(kdfType = 1, kdfMemory = 64, kdfParallelism = 4)
                    .toJson(),
                password = PASSWORD,
            ),
        )
    }

    @Test
    fun `decrypt returns UnsupportedKdf when the iteration count is below the minimum`() = runTest {
        assertEquals(
            DecryptExportResult.UnsupportedKdf,
            createManager().decrypt(
                json = fastKdfEnvelope.toEnvelope().copy(kdfIterations = 4_999).toJson(),
                password = PASSWORD,
            ),
        )
    }

    @Test
    fun `decrypt returns Error when the data is not an EncString`() = runTest {
        assertEquals(
            DecryptExportResult.Error,
            createManager().decrypt(
                json = fastKdfEnvelope.toEnvelope().copy(data = "gibberish").toJson(),
                password = PASSWORD,
            ),
        )
    }

    @Test
    fun `decrypt returns Error when the envelope cannot be parsed`() = runTest {
        assertEquals(
            DecryptExportResult.Error,
            createManager().decrypt(json = "{}", password = PASSWORD),
        )
    }

    @Test
    fun `encrypt produces the envelope the other Bitwarden clients produce`() = runTest {
        val result = createManager(
            secureRandom = FixedSecureRandom(
                values = listOf(SALT_BYTES, VALIDATION_IV_BYTES, DATA_IV_BYTES),
            ),
        )
            .encrypt(json = plaintextExport, password = PASSWORD)

        assertEquals(
            defaultKdfEnvelope.toEnvelope(),
            (result as EncryptExportResult.Success).json.toEnvelope(),
        )
    }

    @Test
    fun `encrypt then decrypt returns the original export`() = runTest {
        val manager = createManager()
        val encrypted = manager.encrypt(json = plaintextExport, password = PASSWORD)

        assertEquals(
            DecryptExportResult.Success(json = plaintextExport),
            manager.decrypt(
                json = (encrypted as EncryptExportResult.Success).json,
                password = PASSWORD,
            ),
        )
    }

    private fun createManager(
        secureRandom: SecureRandom = SecureRandom(),
    ): ExportEncryptionManager = ExportEncryptionManagerImpl(
        dispatcherManager = FakeDispatcherManager(),
        secureRandom = secureRandom,
        uuidManager = uuidManager,
    )

    private fun String.toEnvelope(): EncryptedExportJsonData = json.decodeFromString(this)

    private fun EncryptedExportJsonData.toJson(): String = json.encodeToString(this)

    private fun readResource(name: String): String =
        requireNotNull(javaClass.getResourceAsStream("/exports/$name"))
            .use { it.readBytes().toString(Charsets.UTF_8) }
}

/**
 * A [SecureRandom] that hands out the given [values] in order, so an encrypted export becomes
 * reproducible.
 */
private class FixedSecureRandom(
    private val values: List<ByteArray>,
) : SecureRandom() {
    private var index = 0

    override fun nextBytes(bytes: ByteArray) {
        values[index].copyInto(bytes)
        index++
    }
}

private const val PASSWORD: String = "correct horse battery staple"
private const val VALIDATION_UUID: String = "1c9a8b7d-4e3f-4a2b-8c1d-0e9f8a7b6c5d"
private const val ENC_STRING_SEPARATOR: String = "|"
private const val CIPHERTEXT_INDEX: Int = 1
private val SALT_BYTES: ByteArray = ByteArray(16) { it.toByte() }
private val VALIDATION_IV_BYTES: ByteArray = ByteArray(16) { (it + 16).toByte() }
private val DATA_IV_BYTES: ByteArray = ByteArray(16) { (it + 32).toByte() }
