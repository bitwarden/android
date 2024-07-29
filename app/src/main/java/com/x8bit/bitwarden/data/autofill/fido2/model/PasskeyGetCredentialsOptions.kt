package com.x8bit.bitwarden.data.autofill.fido2.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Models a FIDO 2 credential selection request options received from a Relying Party (RP).
 */
@Serializable
data class PasskeyGetCredentialsOptions(
    @SerialName("challenge")
    val challenge: String,
    @SerialName("allowCredentials")
    val allowCredentials: List<PublicKeyCredentialDescriptor>?,
    @SerialName("rpId")
    val relyingPartyId: String?,
    @SerialName("userVerification")
    val userVerification: UserVerificationRequirement?,
)
