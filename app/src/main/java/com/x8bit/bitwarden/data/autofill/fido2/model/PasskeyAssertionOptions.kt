package com.x8bit.bitwarden.data.autofill.fido2.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Models the request options for a passkey request, based off the spec found at:
 * https://www.w3.org/TR/webauthn-2/#dictionary-assertion-options
 */
@Serializable
data class PasskeyAssertionOptions(
    @SerialName("challenge")
    val challenge: String,
    @SerialName("allowCredentials")
    val allowCredentials: List<PublicKeyCredentialDescriptor>?,
    @SerialName("rpId")
    val relyingPartyId: String?,
    @SerialName("userVerification")
    val userVerification: UserVerificationRequirement = UserVerificationRequirement.PREFERRED,
)
