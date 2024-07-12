package com.x8bit.bitwarden.ui.vault.feature.addedit.util

import com.x8bit.bitwarden.data.autofill.fido2.datasource.network.model.PublicKeyCredentialCreationOptions

/**
 * Returns a mock FIDO 2 [PublicKeyCredentialCreationOptions] object to simulate a credential
 * creation request.
 */
@Suppress("MaxLineLength")
fun createMockPublicKeyCredentialCreationOptions(
    number: Int,
    userVerificationRequirement: PublicKeyCredentialCreationOptions.AuthenticatorSelectionCriteria.UserVerificationRequirement? = null,
) = PublicKeyCredentialCreationOptions(
    authenticatorSelection = PublicKeyCredentialCreationOptions
        .AuthenticatorSelectionCriteria(userVerification = userVerificationRequirement),
    challenge = "mockPublicKeyCredentialCreationOptionsChallenge-$number",
    excludeCredentials = listOf(
        PublicKeyCredentialCreationOptions.PublicKeyCredentialDescriptor(
            type = "mockPublicKeyCredentialDescriptorType-$number",
            id = "mockPublicKeyCredentialDescriptorId-$number",
            transports = listOf("mockPublicKeyCredentialDescriptorTransports-$number"),
        ),
    ),
    pubKeyCredParams = listOf(
        PublicKeyCredentialCreationOptions.PublicKeyCredentialParameters(
            type = "PublicKeyCredentialParametersType-$number",
            alg = number.toLong(),
        ),
    ),
    relyingParty = PublicKeyCredentialCreationOptions.PublicKeyCredentialRpEntity(
        name = "mockPublicKeyCredentialRpEntityName-$number",
        id = "mockPublicKeyCredentialRpEntity-$number",
    ),
    user = PublicKeyCredentialCreationOptions.PublicKeyCredentialUserEntity(
        name = "mockPublicKeyCredentialUserEntityName-$number",
        id = "mockPublicKeyCredentialUserEntityId-$number",
        displayName = "mockPublicKeyCredentialUserEntityDisplayName-$number",
    ),
)
