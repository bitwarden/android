package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class SendVerificationEmailResponseJson {

    /**
     * Models a successful json response.
     *
     * @param captchaBypassToken the bypass token.
     */
    @Serializable
    data class Success(
        @SerialName("emailVerificationToken")
        val emailVerificationToken: String?,
        
        @SerialName("captchaBypassToken")
        val captchaBypassToken: String,
    ) : SendVerificationEmailResponseJson()

    /**
     * Models a json body of a captcha error.
     *
     * @param validationErrors object containing error validations of the response.
     */
    @Serializable
    data class CaptchaRequired(
        @SerialName("validationErrors")
        val validationErrors: ValidationErrors,
    ) : SendVerificationEmailResponseJson() {

        /**
         * Error validations containing a HCaptcha Site Key.
         *
         * @param captchaKeys keys for attempting captcha verification.
         */
        @Serializable
        data class ValidationErrors(
            @SerialName("HCaptcha_SiteKey")
            val captchaKeys: List<String>,
        )
    }

    /**
     * Represents the json body of an invalid request.
     *
     * @param message
     * @param validationErrors a map where each value is a list of error messages for each key.
     * The values in the array should be used for display to the user, since the keys tend to come
     * back as nonsense. (eg: empty string key)
     */
    @Serializable
    data class Invalid(
        @SerialName("message")
        val message: String?,

        @SerialName("validationErrors")
        val validationErrors: Map<String, List<String>>?,
    ) : SendVerificationEmailResponseJson()

    /**
     * A different error with a message.
     */
    @Serializable
    data class Error(
        @SerialName("Message")
        val message: String?,
    ) : SendVerificationEmailResponseJson()
}