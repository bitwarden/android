package com.x8bit.bitwarden.data.autofill.fido2.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Models FIDO 2 credential creation request options received from a Relying Party (RP).
 */
@Serializable
data class PasskeyAttestationOptions(
    @SerialName("authenticatorSelection")
    val authenticatorSelection: AuthenticatorSelectionCriteria,
    @SerialName("challenge")
    val challenge: String,
    @SerialName("excludedCredentials")
    val excludeCredentials: List<PublicKeyCredentialDescriptor> = emptyList(),
    @SerialName("pubKeyCredParams")
    val pubKeyCredParams: List<PublicKeyCredentialParameters>,
    @SerialName("rp")
    val relyingParty: PublicKeyCredentialRpEntity,
    @SerialName("user")
    val user: PublicKeyCredentialUserEntity,
) {

    /**
     * Represents criteria that must be respected when selecting a credential.
     */
    @Serializable
    data class AuthenticatorSelectionCriteria(
        @SerialName("authenticatorAttachment")
        val authenticatorAttachment: AuthenticatorAttachment? = null,
        @SerialName("residentKey")
        val residentKeyRequirement: ResidentKeyRequirement? = null,
        @SerialName("userVerification")
        val userVerification: UserVerificationRequirement = UserVerificationRequirement.PREFERRED,
    ) {
        /**
         * Enum class representing the types of attachments associated with selection criteria.
         */
        @Serializable
        enum class AuthenticatorAttachment {
            @SerialName("platform")
            PLATFORM,

            @SerialName("cross_platform")
            CROSS_PLATFORM,
        }

        /**
         * Enum class indicating the type of authentication expected by the selection criteria.
         */
        @Serializable
        enum class ResidentKeyRequirement {
            /**
             * Resident keys are preferred during selection, if supported.
             */
            @SerialName("preferred")
            PREFERRED,

            /**
             * Resident keys are required during selection.
             */
            @SerialName("required")
            REQUIRED,
        }
    }

    /**
     * Represents parameters for a credential in the creation options.
     */
    @Serializable
    data class PublicKeyCredentialParameters(
        @SerialName("type")
        val type: String,
        @SerialName("alg")
        val alg: Long,
    )

    /**
     * Represents the RP associated with the credential options.
     */
    @Serializable
    data class PublicKeyCredentialRpEntity(
        @SerialName("name")
        val name: String,
        @SerialName("id")
        val id: String,
    )

    /**
     * Represents the user associated with teh credential options.
     */
    @Serializable
    data class PublicKeyCredentialUserEntity(
        @SerialName("name")
        val name: String,
        @SerialName("id")
        val id: String,
        @SerialName("displayName")
        val displayName: String,
    )
}
