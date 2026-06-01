package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents private keys in the vault response.
 *
 * @property signatureKeyPair The signature key pair of the profile.
 * @property publicKeyEncryptionKeyPair The public key encryption key pair of the profile.
 * @property securityState The security state of the profile (nullable).
 */
@Serializable
data class AccountKeysJson(
    @SerialName("signatureKeyPair")
    val signatureKeyPair: SignatureKeyPair?,

    @SerialName("publicKeyEncryptionKeyPair")
    val publicKeyEncryptionKeyPair: PublicKeyEncryptionKeyPair,

    @SerialName("securityState")
    val securityState: SecurityState?,
) {

    /**
     * Represents a signature key pair in the vault response.
     *
     * @property wrappedSigningKey The wrapped signing key of the signature key pair.
     * @property verifyingKey The verifying key of the signature key pair.
     */
    @Serializable
    data class SignatureKeyPair(
        @SerialName("wrappedSigningKey")
        val wrappedSigningKey: String,

        @SerialName("verifyingKey")
        val verifyingKey: String,
    )

    /**
     * Represents a public key encryption key pair in the vault response.
     *
     * @property wrappedPrivateKey The wrapped private key of the public key encryption key
     * pair.
     * @property publicKey The public key of the public key encryption key pair.
     * @property signedPublicKey The signed public key of the public key encryption key pair
     * (nullable).
     */
    @Serializable
    data class PublicKeyEncryptionKeyPair(
        @SerialName("wrappedPrivateKey")
        val wrappedPrivateKey: String,

        @SerialName("publicKey")
        val publicKey: String,

        @SerialName("signedPublicKey")
        val signedPublicKey: String?,
    )

    /**
     * Represents security state in the vault response.
     *
     * @property securityState The security state of the profile.
     * @property securityVersion The security version of the profile.
     */
    @Serializable
    data class SecurityState(
        @SerialName("securityState")
        val securityState: String,

        @SerialName("securityVersion")
        val securityVersion: Int,
    )
}
