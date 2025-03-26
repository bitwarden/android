package com.x8bit.bitwarden.data.autofill.fido2.util

import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.provider.ProviderCreateCredentialRequest

/**
 * Retrieves the [CreatePublicKeyCredentialRequest] from a [ProviderCreateCredentialRequest],
 * otherwise null.
 */
@Suppress("MaxLineLength")
fun ProviderCreateCredentialRequest.getCreatePasskeyCredentialRequestOrNull(): CreatePublicKeyCredentialRequest? =
    callingRequest as? CreatePublicKeyCredentialRequest
