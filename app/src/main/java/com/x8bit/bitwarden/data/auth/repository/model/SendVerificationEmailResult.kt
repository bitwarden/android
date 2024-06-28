package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Models result of sending a verification email.
 */
sealed class SendVerificationEmailResult {
    /**
     * Email sent succeeded.
     *
     * @param emailVerificationToken the token to verify the email.
     * @param captchaToken the captcha bypass token to bypass future captcha verifications.
     */
    data class Success(
        val emailVerificationToken: String?,
        val captchaToken: String
    ) : SendVerificationEmailResult()

    /**
     * Captcha verification is required.
     *
     * @param captchaId the captcha id for performing the captcha verification.
     */
    data class CaptchaRequired(val captchaId: String) : SendVerificationEmailResult()

    /**
     * There was an error sending the email.
     *
     * @param errorMessage a message describing the error.
     */
    data class Error(val errorMessage: String?) : SendVerificationEmailResult()
}