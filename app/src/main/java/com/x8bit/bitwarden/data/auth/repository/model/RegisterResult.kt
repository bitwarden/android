package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Models result of registering a new account.
 */
sealed class RegisterResult {
    /**
     * Register succeeded.
     *
     * @param captchaToken the captcha bypass token to bypass future captcha verifications.
     */
    data class Success(val captchaToken: String) : RegisterResult()

    /**
     * Captcha verification is required.
     *
     * @param captchaId the captcha id for performing the captcha verification.
     */
    data class CaptchaRequired(val captchaId: String) : RegisterResult()

    /**
     * There was an error logging in.
     *
     * @param errorMessage a message describing the error.
     */
    data class Error(val errorMessage: String?) : RegisterResult()

    /**
     * Password hash was found in a data breach.
     */
    data object DataBreachFound : RegisterResult()

    /**
     * Password hash was found to be weak.
     */
    data object WeakPassword : RegisterResult()

    /**
     * Password hash was found in a data breach and found to be weak.
     */
    data object DataBreachAndWeakPassword : RegisterResult()
}
