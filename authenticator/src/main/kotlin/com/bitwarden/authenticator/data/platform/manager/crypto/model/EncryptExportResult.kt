package com.bitwarden.authenticator.data.platform.manager.crypto.model

/**
 * Represents the result of encrypting an export.
 */
sealed class EncryptExportResult {

    /**
     * The export was encrypted, producing the given password-protected [json] document.
     */
    data class Success(val json: String) : EncryptExportResult()

    /**
     * The export could not be encrypted.
     */
    data object Error : EncryptExportResult()
}
