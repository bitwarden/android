package com.x8bit.bitwarden.data.auth.datasource.network.model

/**
 * Models result of logging in.
 *
 * TODO: Add more detail to these cases to expose server error messages (BIT-320)
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
    data object Error : LoginResult()
}
