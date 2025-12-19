package com.bitwarden.network.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
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
     * @property accountKeys The user's account keys, which include the signature key pair and
     * public key encryption key pair. This is temporarily nullable to support older accounts that
     * have not been upgraded to use account keys instead of the deprecated `PrivateKey` field.
     * @property shouldForcePasswordReset Whether or not the app must force a password reset.
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

        @Deprecated(
            message = "Use `accountKeys` instead.",
            replaceWith = ReplaceWith(
                "loginResponse.accountKeys?.publicKeyEncryptionKeyPair?.wrappedPrivateKey",
            ),
        )
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

        @SerialName("AccountKeys")
        val accountKeys: AccountKeysJson?,

        @SerialName("ForcePasswordReset")
        val shouldForcePasswordReset: Boolean,

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
     * Models json body of an invalid request.
     *
     * This model supports older versions of the error response model that used lower-case keys.
     */
    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    data class Invalid(
        @JsonNames("errorModel")
        @SerialName("ErrorModel")
        private val errorModel: ErrorModel?,
    ) : GetTokenResponseJson() {

        /**
         * The error message returned from the server, or null.
         */
        val errorMessage: String? get() = errorModel?.errorMessage

        /**
         * The type of invalid responses that can be received.
         */
        sealed class InvalidType {
            /**
             * Represents an invalid response indicating that a new device verification is required.
             */
            data object NewDeviceVerification : InvalidType()

            /**
             * Represents an invalid response indicating that a new device verification is required.
             */
            data object EncryptionKeyMigrationRequired : InvalidType()

            /**
             * Represents generic invalid response
             */
            data object GenericInvalid : InvalidType()
        }

        val invalidType: InvalidType
            get() = if (errorMessage?.lowercase() == "new device verification required") {
                InvalidType.NewDeviceVerification
            } else if (errorMessage
                    ?.lowercase()
                    ?.contains(
                        "encryption key migration is required. please log in to the web vault at",
                    ) == true
            ) {
                InvalidType.EncryptionKeyMigrationRequired
            } else {
                InvalidType.GenericInvalid
            }

        /**
         * The error body of an invalid request containing a message.
         *
         * This model supports older versions of the error response model that used lower-case
         * keys.
         */
        @Serializable
        data class ErrorModel(
            @JsonNames("message")
            @SerialName("Message")
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

        @SerialName("SsoEmail2faSessionToken")
        val ssoToken: String?,
    ) : GetTokenResponseJson()
}
