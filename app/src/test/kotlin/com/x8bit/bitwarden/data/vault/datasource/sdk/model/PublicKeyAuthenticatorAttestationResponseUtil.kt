package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.fido.AuthenticatorAttestationResponse
import com.bitwarden.fido.ClientExtensionResults
import com.bitwarden.fido.CredPropsResult
import com.bitwarden.fido.PublicKeyCredentialAuthenticatorAttestationResponse
import com.bitwarden.fido.SelectedCredential

/**
 * Creates a mock [PublicKeyCredentialAuthenticatorAttestationResponse] for testing.
 */
fun createMockPublicKeyAttestationResponse(number: Int) =
    PublicKeyCredentialAuthenticatorAttestationResponse(
        id = "mockId",
        rawId = "0987654321".toByteArray(),
        ty = "mockTy",
        authenticatorAttachment = "mockAuthenticatorAttachment",
        clientExtensionResults = ClientExtensionResults(
            credProps = CredPropsResult(
                rk = true,
            ),
        ),
        response = AuthenticatorAttestationResponse(
            clientDataJson = "mockClientDataJson".toByteArray(),
            authenticatorData = "mockAuthenticatorData".toByteArray(),
            publicKey = "mockPublicKey".toByteArray(),
            publicKeyAlgorithm = 0L,
            attestationObject = "mockAttestationObject".toByteArray(),
            transports = emptyList(),
        ),
        selectedCredential = SelectedCredential(
            createMockCipherView(number),
            createMockFido2CredentialView(number),
        ),
    )
