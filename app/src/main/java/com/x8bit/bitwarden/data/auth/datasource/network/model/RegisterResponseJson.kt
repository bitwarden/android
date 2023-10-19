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
}
