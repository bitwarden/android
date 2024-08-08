package com.x8bit.bitwarden.data.autofill.fido2.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Models a FIDO 2 public key credential.
 */
@Serializable
data class Fido2PublicKeyCredential(
    @SerialName("id")
    val id: String,
    @SerialName("rawId")
    val rawId: String,
    @SerialName("type")
    val type: String,
    @SerialName("authenticatorAttachment")
    val authenticatorAttachment: String?,
    @SerialName("response")
    val response: Fido2AssertionResponse,
    @SerialName("clientExtensionResults")
    val clientExtensionResults: ClientExtensionResults,
) {

    /**
     * Models a FIDO 2 public key assertion response.
     */
    @Serializable
    data class Fido2AssertionResponse(
        @SerialName("clientDataJSON")
        val clientDataJson: String?,
        @SerialName("authenticatorData")
        val authenticatorData: String,
        @SerialName("signature")
        val signature: String,
        @SerialName("userHandle")
        val userHandle: String?,
    )

    /**
     * Models FIDO 2 credential properties provided by a client.
     */
    @Serializable
    data class ClientExtensionResults(
        @SerialName("credProps")
        val credentialProperties: CredentialProperties?,
    ) {
        /**
         * Models the FIDO 2 credential properties provided by a client.
         */
        @Serializable
        data class CredentialProperties(
            @SerialName("rk")
            val residentKey: Boolean?,
        )
    }
}
