package com.x8bit.bitwarden.data.vault.datasource.sdk.util

import android.util.Base64
import com.bitwarden.fido.PublicKeyCredentialAuthenticatorAssertionResponse
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2PublicKeyCredential

/**
 * Converts the Bitwarden SDK response to a [Fido2PublicKeyCredential] that can be serialized into
 * the expected system JSON.
 */
fun PublicKeyCredentialAuthenticatorAssertionResponse.toAndroidFido2PublicKeyCredential() =
    Fido2PublicKeyCredential(
        id = id,
        rawId = rawId.base64EncodeForFido2Response(),
        type = this.ty,
        authenticatorAttachment = authenticatorAttachment,
        response = Fido2PublicKeyCredential.Fido2AssertionResponse(
            clientDataJson = response.clientDataJson.base64EncodeForFido2Response(),
            authenticatorData = response.authenticatorData.base64EncodeForFido2Response(),
            signature = response.signature.base64EncodeForFido2Response(),
            userHandle = response.userHandle.base64EncodeForFido2Response(),
        ),
        clientExtensionResults = Fido2PublicKeyCredential.ClientExtensionResults(
            credentialProperties = clientExtensionResults.credProps?.let { credProps ->
                Fido2PublicKeyCredential
                    .ClientExtensionResults
                    .CredentialProperties(
                        residentKey = credProps.rk ?: true,
                    )
            },
        ),
    )

private fun ByteArray.base64EncodeForFido2Response(): String =
    Base64.encodeToString(
        this,
        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
    )
