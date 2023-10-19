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
     * There was an error logging in.
     */
    data class Error(val errorMessage: String?) : LoginResult()
}
