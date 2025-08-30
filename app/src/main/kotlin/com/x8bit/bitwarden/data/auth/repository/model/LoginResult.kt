package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Models result of logging in.
 */
sealed class LoginResult {
    /**
     * Login succeeded.
     */
    data object Success : LoginResult()

    /**
     * Encryption key migration is required.
     */
    data object EncryptionKeyMigrationRequired : LoginResult()

    /**
     * Two-factor verification is required.
     */
    data object TwoFactorRequired : LoginResult()

    /**
     * User should confirm KeyConnector domain
     */
    data class ConfirmKeyConnectorDomain(
        val domain: String,
    ) : LoginResult()

    /**
     * There was an error logging in.
     */
    data class Error(
        val errorMessage: String?,
        val error: Throwable?,
    ) : LoginResult()

    /**
     * There was an error while logging into an unofficial Bitwarden server.
     */
    data object UnofficialServerError : LoginResult()

    /**
     * There was an error in validating the certificate chain for the server
     */
    data object CertificateError : LoginResult()

    /**
     * New device verification is required
     */
    data class NewDeviceVerification(val errorMessage: String?) : LoginResult()
}
