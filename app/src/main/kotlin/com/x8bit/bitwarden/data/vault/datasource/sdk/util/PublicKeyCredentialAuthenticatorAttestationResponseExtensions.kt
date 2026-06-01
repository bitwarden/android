package com.x8bit.bitwarden.data.vault.datasource.sdk.util

import android.util.Base64
import com.bitwarden.fido.PublicKeyCredentialAuthenticatorAttestationResponse
import com.x8bit.bitwarden.data.credentials.model.Fido2AttestationResponse

private const val BINANCE_PACKAGE_NAME = "com.binance.dev"

/**
 * Converts the SDK attestation response to a [Fido2AttestationResponse] that can be serialized into
 * the expected system JSON.
 */
fun PublicKeyCredentialAuthenticatorAttestationResponse.toAndroidAttestationResponse(
    callingPackageName: String?,
): Fido2AttestationResponse {
    val registrationResponse = Fido2AttestationResponse.RegistrationResponse(
        clientDataJson = response.clientDataJson.base64EncodeForFido2Response(),
        attestationObject = response.attestationObject.base64EncodeForFido2Response(),
        transports = response.transports.takeUnless {
            // Setting transports as null, otherwise Binance labels the passkey broken
            // PM-26734 remove this flow if not necessary anymore
            callingPackageName == BINANCE_PACKAGE_NAME
        },
        publicKeyAlgorithm = response.publicKeyAlgorithm,
        publicKey = response.publicKey?.base64EncodeForFido2Response(),
        authenticatorData = response.authenticatorData.base64EncodeForFido2Response(),
    )

    return Fido2AttestationResponse(
        id = id,
        type = ty,
        rawId = rawId.base64EncodeForFido2Response(),
        response = registrationResponse,
        clientExtensionResults = clientExtensionResults
            .credProps
            ?.rk
            ?.let { residentKey ->
                Fido2AttestationResponse.ClientExtensionResults(
                    credentialProperties = Fido2AttestationResponse
                        .ClientExtensionResults
                        .CredentialProperties(residentKey = residentKey),
                )
            } ?: Fido2AttestationResponse.ClientExtensionResults(),
        authenticatorAttachment = authenticatorAttachment,
    )
}

/**
 * Attestation response fields of type [ByteArray] must be base 64 encoded in a url safe format
 * without newline or padding symbols according to the FIDO 2 spec.
 */
private fun ByteArray.base64EncodeForFido2Response(): String =
    Base64.encodeToString(
        this,
        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
    )
