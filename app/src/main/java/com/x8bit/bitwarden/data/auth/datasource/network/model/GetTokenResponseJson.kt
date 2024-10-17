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
        @SerialName("access_token")
        val accessToken: String,

        @SerialName("refresh_token")
        val refreshToken: String,

        @SerialName("token_type")
        val tokenType: String,

        @SerialName("expires_in")
        val expiresInSeconds: Int,

        @SerialName("Key")
        val key: String?,

        @SerialName("PrivateKey")
        val privateKey: String?,

        @SerialName("Kdf")
        val kdfType: KdfTypeJson,

        @SerialName("KdfIterations")
        val kdfIterations: Int?,

        @SerialName("KdfMemory")
        val kdfMemory: Int?,

        @SerialName("KdfParallelism")
        val kdfParallelism: Int?,

        @SerialName("ForcePasswordReset")
        val shouldForcePasswordReset: Boolean,

        @SerialName("ResetMasterPassword")
        val shouldResetMasterPassword: Boolean,

        @SerialName("TwoFactorToken")
        val twoFactorToken: String?,

        @SerialName("MasterPasswordPolicy")
        val masterPasswordPolicyOptions: MasterPasswordPolicyOptionsJson?,

        @SerialName("UserDecryptionOptions")
        val userDecryptionOptions: UserDecryptionOptionsJson?,

        @SerialName("KeyConnectorUrl")
        val keyConnectorUrl: String?,
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
        val errorModel: ErrorModel?,
        @SerialName("errorModel")
        val legacyErrorModel: LegacyErrorModel?,
    ) : GetTokenResponseJson() {

        /**
         * The error message returned from the server, or null.
         */
        val errorMessage: String?
            get() = errorModel?.errorMessage ?: legacyErrorModel?.errorMessage

        /**
         * The error body of an invalid request containing a message.
         */
        @Serializable
        data class ErrorModel(
            @SerialName("Message")
            val errorMessage: String,
        )

        /**
         * The legacy error body of an invalid request containing a message.
         *
         * This model is used to support older versions of the error response model that used
         * lower-case keys.
         */
        @Serializable
        data class LegacyErrorModel(
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
        @SerialName("TwoFactorProviders2")
        val authMethodsData: Map<TwoFactorAuthMethod, JsonObject?>,

        @SerialName("TwoFactorProviders")
        val twoFactorProviders: List<String>?,

        @SerialName("CaptchaBypassToken")
        val captchaToken: String?,

        @SerialName("SsoEmail2faSessionToken")
        val ssoToken: String?,
    ) : GetTokenResponseJson()
}
