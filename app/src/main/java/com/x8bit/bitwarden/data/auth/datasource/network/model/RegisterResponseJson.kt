package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Models response bodies for the register request.
 */
@Serializable
sealed class RegisterResponseJson {

    /**
     * Models a successful json response of the register request.
     *
     * @param captchaBypassToken the bypass token.
     */
    @Serializable
    data class Success(
        @SerialName("captchaBypassToken")
        val captchaBypassToken: String,
    ) : RegisterResponseJson()

    /**
     * Models a json body of a captcha error.
     *
     * @param validationErrors object containing error validations of the response.
     */
    @Serializable
    data class CaptchaRequired(
        @SerialName("validationErrors")
        val validationErrors: ValidationErrors,
    ) : RegisterResponseJson() {

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
     * Represents the json body of an invalid register request.
     *
     * @param validationErrors a map where each value is a list of error messages for each key.
     * The values in the array should be used for display to the user, since the keys tend to come
     * back as nonsense. (eg: empty string key)
     */
    @Serializable
    data class Invalid(
        @SerialName("message")
        private val invalidMessage: String? = null,

        @SerialName("Message")
        private val errorMessage: String? = null,

        @SerialName("validationErrors")
        val validationErrors: Map<String, List<String>>?,
    ) : RegisterResponseJson() {
        /**
         * A generic error message.
         */
        val message: String? get() = invalidMessage ?: errorMessage
    }
}
