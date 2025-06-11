package com.x8bit.bitwarden.ui.vault.feature.addedit.util

import com.x8bit.bitwarden.data.credentials.model.PasskeyAttestationOptions
import com.x8bit.bitwarden.data.credentials.model.PublicKeyCredentialDescriptor
import com.x8bit.bitwarden.data.credentials.model.UserVerificationRequirement

/**
 * Returns a mock FIDO 2 [PasskeyAttestationOptions] object to simulate a credential
 * creation request.
 */
@Suppress("MaxLineLength")
fun createMockPasskeyAttestationOptions(
    number: Int,
    userVerificationRequirement: UserVerificationRequirement =
        UserVerificationRequirement.PREFERRED,
    relyingPartyId: String = "mockPublicKeyCredentialRpEntity-$number",
) = PasskeyAttestationOptions(
    authenticatorSelection = PasskeyAttestationOptions
        .AuthenticatorSelectionCriteria(userVerification = userVerificationRequirement),
    challenge = "mockPublicKeyCredentialCreationOptionsChallenge-$number",
    excludeCredentials = listOf(
        PublicKeyCredentialDescriptor(
            type = "mockPublicKeyCredentialDescriptorType-$number",
            id = "mockPublicKeyCredentialDescriptorId-$number",
            transports = listOf("mockPublicKeyCredentialDescriptorTransports-$number"),
        ),
    ),
    pubKeyCredParams = listOf(
        PasskeyAttestationOptions.PublicKeyCredentialParameters(
            type = "PublicKeyCredentialParametersType-$number",
            alg = number.toLong(),
        ),
    ),
    relyingParty = PasskeyAttestationOptions.PublicKeyCredentialRpEntity(
        name = "mockPublicKeyCredentialRpEntityName-$number",
        id = relyingPartyId,
    ),
    user = PasskeyAttestationOptions.PublicKeyCredentialUserEntity(
        name = "mockPublicKeyCredentialUserEntityName-$number",
        id = "mockPublicKeyCredentialUserEntityId-$number",
        displayName = "mockPublicKeyCredentialUserEntityDisplayName-$number",
    ),
)
