package com.bitwarden.authenticator.data.platform.manager.crypto.model

/**
 * Represents the result of decrypting a password-protected export.
 */
sealed class DecryptExportResult {

    /**
     * The export was decrypted, producing the given plaintext [json] document.
     */
    data class Success(val json: String) : DecryptExportResult()

    /**
     * The password does not match the one the file was encrypted with.
     */
    data object IncorrectPassword : DecryptExportResult()

    /**
     * The file was encrypted with key derivation settings this application cannot reproduce.
     */
    data object UnsupportedKdf : DecryptExportResult()

    /**
     * The file could not be decrypted.
     */
    data object Error : DecryptExportResult()
}
