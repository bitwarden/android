package com.bitwarden.authenticator.data.platform.manager.crypto

import com.bitwarden.authenticator.data.authenticator.manager.model.EncryptedExportJsonData
import com.bitwarden.authenticator.data.platform.manager.crypto.model.DecryptExportResult
import com.bitwarden.authenticator.data.platform.manager.crypto.model.EncryptExportResult
import com.bitwarden.core.data.manager.UuidManager
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Base64

private const val KDF_TYPE_PBKDF2: Int = 0
private const val PBKDF2_ITERATIONS: Int = 600_000
private const val PBKDF2_MIN_ITERATIONS: Int = 5_000
private const val PBKDF2_MAX_ITERATIONS: Int = 10_000_000
private const val SALT_SIZE_BYTES: Int = 16
private const val IV_SIZE_BYTES: Int = 16
private const val PRETTY_PRINT_INDENT: String = "  "

/**
 * The only field of an export that has to be read before a password is known.
 */
@Serializable
private data class PasswordProtectionProbe(
    val encrypted: Boolean = false,
)

/**
 * Default implementation of [ExportEncryptionManager].
 */
class ExportEncryptionManagerImpl(
    private val dispatcherManager: DispatcherManager,
    private val secureRandom: SecureRandom,
    private val uuidManager: UuidManager,
) : ExportEncryptionManager {

    private val envelopeJson: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = true
        prettyPrintIndent = PRETTY_PRINT_INDENT
    }

    override fun isPasswordProtected(json: String): Boolean = try {
        envelopeJson.decodeFromString<PasswordProtectionProbe>(json).encrypted
    } catch (_: SerializationException) {
        false
    }

    override suspend fun encrypt(json: String, password: String): EncryptExportResult =
        withContext(dispatcherManager.io) {
            val salt = Base64
                .getEncoder()
                .encodeToString(ByteArray(SALT_SIZE_BYTES).also(secureRandom::nextBytes))
            try {
                val key = ExportEnvelopeCrypto.deriveStretchedKey(
                    password = password,
                    salt = salt,
                    iterations = PBKDF2_ITERATIONS,
                )
                EncryptExportResult.Success(
                    json = envelopeJson.encodeToString(
                        EncryptedExportJsonData(
                            encrypted = true,
                            passwordProtected = true,
                            salt = salt,
                            kdfType = KDF_TYPE_PBKDF2,
                            kdfIterations = PBKDF2_ITERATIONS,
                            kdfMemory = null,
                            kdfParallelism = null,
                            encKeyValidation = ExportEnvelopeCrypto.encrypt(
                                plaintext = uuidManager.generateUuid(),
                                iv = nextIv(),
                                key = key,
                            ),
                            data = ExportEnvelopeCrypto.encrypt(
                                plaintext = json,
                                iv = nextIv(),
                                key = key,
                            ),
                        ),
                    ),
                )
            } catch (_: GeneralSecurityException) {
                EncryptExportResult.Error
            } catch (_: SerializationException) {
                EncryptExportResult.Error
            }
        }

    override suspend fun decrypt(json: String, password: String): DecryptExportResult =
        withContext(dispatcherManager.io) {
            val envelope = try {
                envelopeJson.decodeFromString<EncryptedExportJsonData>(json)
            } catch (_: SerializationException) {
                return@withContext DecryptExportResult.Error
            }
            if (envelope.kdfType != KDF_TYPE_PBKDF2 ||
                envelope.kdfIterations !in PBKDF2_MIN_ITERATIONS..PBKDF2_MAX_ITERATIONS
            ) {
                return@withContext DecryptExportResult.UnsupportedKdf
            }
            val validation = ExportEnvelopeCrypto.parse(envelope.encKeyValidation)
                ?: return@withContext DecryptExportResult.Error
            val data = ExportEnvelopeCrypto.parse(envelope.data)
                ?: return@withContext DecryptExportResult.Error

            try {
                val key = ExportEnvelopeCrypto.deriveStretchedKey(
                    password = password,
                    salt = envelope.salt,
                    iterations = envelope.kdfIterations,
                )
                // The validation field exists to tell a wrong password from a damaged file.
                ExportEnvelopeCrypto.decrypt(encString = validation, key = key)
                    ?: return@withContext DecryptExportResult.IncorrectPassword
                ExportEnvelopeCrypto
                    .decrypt(encString = data, key = key)
                    ?.let { DecryptExportResult.Success(json = it) }
                    ?: DecryptExportResult.Error
            } catch (_: GeneralSecurityException) {
                DecryptExportResult.Error
            }
        }

    private fun nextIv(): ByteArray = ByteArray(IV_SIZE_BYTES).also(secureRandom::nextBytes)
}
