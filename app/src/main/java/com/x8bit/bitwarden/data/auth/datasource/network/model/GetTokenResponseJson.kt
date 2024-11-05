package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Models response bodies from the get token request.
 */
sealed class GetTokenResponseJson {
    /**
     * Models json response of the get token request.
     *
     * @property accessToken The user's access token.
     * @property refreshToken The user's refresh token.
     * @property tokenType The type of token (ex: "Bearer").
     * @property expiresInSeconds The amount of time (in seconds) before the [accessToken] expires.
     * @property key The user's key.
     * @property privateKey The user's private key.
     * @property kdfType The KDF type.
     * @property kdfIterations The number of iterations when calculating a user's password.
     * @property kdfMemory The amount of memory to use when calculating a password hash (MB).
     * @property kdfParallelism The number of threads to use when calculating a password hash.
     * @property shouldForcePasswordReset Whether or not the app must force a password reset.
     * @property shouldResetMasterPassword Whether or not the user is required to reset their
     * master password.
     * @property twoFactorToken If the user has chosen to remember the two-factor authorization,
     * this token will be cached and used for future auth requests.
     * @property masterPasswordPolicyOptions The options available for a user's master password.
     * @property userDecryptionOptions The options available to a user for decryption.
     * @property keyConnectorUrl URL to the user's key connector.
     */
    @Serializable
    data class Success(
        @SerialName("accessToken")
        val accessToken: String,

        @SerialName("refreshToken")
        val refreshToken: String,

        @SerialName("tokenType")
        val tokenType: String,

        @SerialName("expiresIn")
        val expiresInSeconds: Int,

        @SerialName("key")
        val key: String?,

        @SerialName("privateKey")
        val privateKey: String?,

        @SerialName("kdf")
        val kdfType: KdfTypeJson,

        @SerialName("kdfIterations")
        val kdfIterations: Int?,

        @SerialName("kdfMemory")
        val kdfMemory: Int?,

        @SerialName("kdfParallelism")
        val kdfParallelism: Int?,

        @SerialName("forcePasswordReset")
        val shouldForcePasswordReset: Boolean,

        @SerialName("resetMasterPassword")
        val shouldResetMasterPassword: Boolean,

        @SerialName("twoFactorToken")
        val twoFactorToken: String?,

        @SerialName("masterPasswordPolicy")
        val masterPasswordPolicyOptions: MasterPasswordPolicyOptionsJson?,

        @SerialName("userDecryptionOptions")
        val userDecryptionOptions: UserDecryptionOptionsJson?,

        @SerialName("keyConnectorUrl")
        val keyConnectorUrl: String?,
    ) : GetTokenResponseJson()

    /**
     * Models json body of a captcha error.
     */
    @Serializable
    data class CaptchaRequired(
        @SerialName("hCaptchaSiteKey")
        val captchaKey: String,
    ) : GetTokenResponseJson()

    /**
     * Models json body of an invalid request.
     */
    @Serializable
    data class Invalid(
        @SerialName("errorModel")
        val errorModel: ErrorModel?,
    ) : GetTokenResponseJson() {

        /**
         * The error body of an invalid request containing a message.
         */
        @Serializable
        data class ErrorModel(
            @SerialName("message")
            val errorMessage: String,
        )
    }

    /**
     * Models json body of a two-factor error.
     *
     * @property authMethodsData A blob of data formatted as:
     * `{"1":{"Email":"sh*****@example.com"},"0":{"Email":null}}`
     * The keys are the raw values of the [TwoFactorAuthMethod],
     * and the map is any extra information for the method.
     * @property captchaToken The captcha token used in the second
     * login attempt if the user has already passed a captcha
     * authentication in the first attempt.
     * @property ssoToken  If the user is logging on via Single
     * Sign On, they'll need this value to complete authentication
     * after entering their two-factor code.
     */
    @Serializable
    data class TwoFactorRequired(
        @SerialName("twoFactorProviders2")
        val authMethodsData: Map<TwoFactorAuthMethod, JsonObject?>,

        @SerialName("twoFactorProviders")
        val twoFactorProviders: List<String>?,

        @SerialName("captchaBypassToken")
        val captchaToken: String?,

        @SerialName("ssoEmail2faSessionToken")
        val ssoToken: String?,
    ) : GetTokenResponseJson()
}
