package com.x8bit.bitwarden.ui.vault.feature.addedit.util

import com.x8bit.bitwarden.data.autofill.fido2.model.PublicKeyCredentialDescriptor
import com.x8bit.bitwarden.data.autofill.fido2.model.PasskeyAssertionOptions

/**
 * Returns a mock FIDO 2 [PasskeyAssertionOptions] object to simulate a credential
 * creation request.
 */
fun createMockPasskeyAssertionOptions(
    number: Int,
) = PasskeyAssertionOptions(
    challenge = "mockChallenge-$number",
    allowCredentials = listOf(
        PublicKeyCredentialDescriptor(
            type = "mockPublicKeyCredentialDescriptorType-$number",
            id = "mockPublicKeyCredentialDescriptorId-$number",
            transports = listOf("mockPublicKeyCredentialDescriptorTransports-$number"),
        ),
    ),
    relyingPartyId = "mockRelyingPartyId-$number",
    userVerification = "mockUserVerification-$number",
)
