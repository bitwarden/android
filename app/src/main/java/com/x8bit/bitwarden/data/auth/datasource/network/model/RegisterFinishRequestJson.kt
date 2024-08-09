package com.x8bit.bitwarden.data.auth.datasource.network.model

import com.x8bit.bitwarden.data.auth.datasource.network.model.RegisterFinishRequestJson.Keys
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for register.
 *
 * @param email the email to be registered.
 * @param emailVerificationToken token used to finish the registration process.
 * @param masterPasswordHash the master password (encrypted).
 * @param masterPasswordHint the hint for the master password (nullable).
 * @param captchaResponse the captcha bypass token.
 * @param userSymmetricKey the user key for the request (encrypted).
 * @param userAsymmetricKeys a [Keys] object containing public and private keys.
 * @param kdfType the kdf type represented as an [Int].
 * @param kdfIterations the number of kdf iterations.
 */
@Serializable
data class RegisterFinishRequestJson(
    @SerialName("email")
    val email: String,

    @SerialName("emailVerificationToken")
    val emailVerificationToken: String,

    @SerialName("masterPasswordHash")
    val masterPasswordHash: String,

    @SerialName("masterPasswordHint")
    val masterPasswordHint: String?,

    @SerialName("captchaResponse")
    val captchaResponse: String?,

    @SerialName("userSymmetricKey")
    val userSymmetricKey: String,

    @SerialName("userAsymmetricKeys")
    val userAsymmetricKeys: Keys,

    @SerialName("kdf")
    val kdfType: KdfTypeJson,

    @SerialName("kdfIterations")
    val kdfIterations: UInt,
) {

    /**
     * A keys object containing public and private keys.
     *
     * @param publicKey the public key (encrypted).
     * @param encryptedPrivateKey the private key (encrypted).
     */
    @Serializable
    data class Keys(
        @SerialName("publicKey")
        val publicKey: String,

        @SerialName("encryptedPrivateKey")
        val encryptedPrivateKey: String,
    )
}
