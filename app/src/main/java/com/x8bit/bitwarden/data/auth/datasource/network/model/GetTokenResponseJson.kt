package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Models response bodies from the get token request.
 */
sealed class GetTokenResponseJson {
    /**
     * Models json response of the get token request.
     *
     * @param accessToken the access token.
     */
    @Serializable
    data class Success(
        @SerialName("access_token")
        val accessToken: String,
    ) : GetTokenResponseJson()

    /**
     * Models json body of a captcha error.
     */
    @Serializable
    data class CaptchaRequired(
        @SerialName("HCaptcha_SiteKey")
        val captchaKey: String,
    ) : GetTokenResponseJson()

    /**
     * Models json body of an invalid request.
     */
    @Serializable
    data class Invalid(
        @SerialName("ErrorModel")
        val errorModel: ErrorModel,
    ) : GetTokenResponseJson() {

        /**
         * The error body of an invalid request containing a message.
         */
        @Serializable
        data class ErrorModel(
            @SerialName("Message")
            val errorMessage: String,
        )
    }
}
