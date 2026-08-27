package com.bitwarden.authenticator.data.authenticator.manager.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Models a password-protected Bitwarden export.
 *
 * The field names and layout match the envelope every other Bitwarden client writes, so a file
 * produced here can be imported elsewhere and vice versa. The [encKeyValidation] and [data] values
 * are Bitwarden `EncString`s of the form `2.[iv]|[ciphertext]|[mac]`.
 *
 * @property encrypted Always `true`; distinguishes this envelope from a plaintext export.
 * @property passwordProtected Always `true`; the key is derived from a password rather than shared.
 * @property salt Base64 of the KDF salt. The base64 text itself is the salt input, not its bytes.
 * @property kdfType `0` for PBKDF2, `1` for Argon2id.
 * @property kdfIterations Iteration count used to derive the key material.
 * @property kdfMemory Argon2id memory in MiB; `null` for PBKDF2.
 * @property kdfParallelism Argon2id parallelism; `null` for PBKDF2.
 * @property encKeyValidation An encrypted random UUID used to verify the derived key.
 * @property data The encrypted [ExportJsonData] document.
 */
@Serializable
data class EncryptedExportJsonData(
    val encrypted: Boolean,
    val passwordProtected: Boolean,
    val salt: String,
    val kdfType: Int,
    val kdfIterations: Int,
    val kdfMemory: Int? = null,
    val kdfParallelism: Int? = null,
    @SerialName("encKeyValidation_DO_NOT_EDIT")
    val encKeyValidation: String,
    val data: String,
)
