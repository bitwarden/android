package com.bitwarden.authenticator.data.platform.manager.crypto

import com.bitwarden.authenticator.data.platform.manager.crypto.model.DecryptExportResult
import com.bitwarden.authenticator.data.platform.manager.crypto.model.EncryptExportResult

/**
 * Responsible for producing and opening password-protected Bitwarden exports.
 *
 * Implementations must stay byte-compatible with the envelope written by the other Bitwarden
 * clients so exports can be moved between them.
 */
interface ExportEncryptionManager {

    /**
     * Returns `true` when [json] is a password-protected export envelope, and `false` when it is
     * plaintext or cannot be recognized at all.
     */
    fun isPasswordProtected(json: String): Boolean

    /**
     * Wraps the plaintext export [json] in an envelope encrypted with the given [password].
     */
    suspend fun encrypt(json: String, password: String): EncryptExportResult

    /**
     * Unwraps the password-protected export [json] using the given [password].
     */
    suspend fun decrypt(json: String, password: String): DecryptExportResult
}
