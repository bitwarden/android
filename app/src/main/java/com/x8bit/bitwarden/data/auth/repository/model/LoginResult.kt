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
     * Captcha verification is required.
     */
    data class CaptchaRequired(val captchaId: String) : LoginResult()

    /**
     * Two-factor verification is required.
     */
    data object TwoFactorRequired : LoginResult()

    /**
     * There was an error logging in.
     */
    data class Error(val errorMessage: String?) : LoginResult()

    /**
     * There was an error while logging into an unofficial Bitwarden server.
     */
    data object UnofficialServerError : LoginResult()

    /**
     * There was an error in validating the certificate chain for the server
     */
    data object CertificateError : LoginResult()
}
