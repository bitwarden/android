package com.x8bit.bitwarden.data.vault.datasource.sdk.util

import android.util.Base64
import com.bitwarden.fido.PublicKeyCredentialAuthenticatorAttestationResponse
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2AttestationResponse

/**
 * Converts the SDK attestation response to a [Fido2AttestationResponse] that can be serialized into
 * the expected system JSON.
 */
@Suppress("MaxLineLength")
fun PublicKeyCredentialAuthenticatorAttestationResponse.toAndroidAttestationResponse(): Fido2AttestationResponse =
    Fido2AttestationResponse(
        id = id,
        type = ty,
        rawId = rawId.base64EncodeForFido2Response(),
        response = Fido2AttestationResponse.RegistrationResponse(
            clientDataJson = response.clientDataJson.base64EncodeForFido2Response(),
            attestationObject = response.attestationObject.base64EncodeForFido2Response(),
            transports = response.transports,
            publicKeyAlgorithm = response.publicKeyAlgorithm,
            publicKey = response.publicKey?.base64EncodeForFido2Response(),
            authenticatorData = response.authenticatorData.base64EncodeForFido2Response(),
        ),
        clientExtensionResults = clientExtensionResults
            .credProps
            ?.rk
            ?.let { residentKey ->
                Fido2AttestationResponse.ClientExtensionResults(
                    credentialProperties = Fido2AttestationResponse
                        .ClientExtensionResults
                        .CredentialProperties(residentKey = residentKey),
                )
            },
        authenticatorAttachment = authenticatorAttachment,
    )

/**
 * Attestation response fields of type [ByteArray] must be base 64 encoded in a url safe format
 * without newline or padding symbols according to the FIDO 2 spec.
 */
private fun ByteArray.base64EncodeForFido2Response(): String =
    Base64.encodeToString(
        this,
        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
    )
