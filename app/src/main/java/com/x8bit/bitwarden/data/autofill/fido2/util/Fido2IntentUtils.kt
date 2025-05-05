package com.x8bit.bitwarden.data.autofill.fido2.util

import android.content.Intent
import android.os.Build
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.provider.ProviderCreateCredentialRequest
import androidx.credentials.provider.ProviderGetCredentialRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CreateCredentialRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2GetCredentialsRequest
import com.x8bit.bitwarden.data.platform.util.isBuildVersionBelow
import com.x8bit.bitwarden.ui.platform.manager.intent.EXTRA_KEY_CIPHER_ID
import com.x8bit.bitwarden.ui.platform.manager.intent.EXTRA_KEY_CREDENTIAL_ID
import com.x8bit.bitwarden.ui.platform.manager.intent.EXTRA_KEY_USER_ID
import com.x8bit.bitwarden.ui.platform.manager.intent.EXTRA_KEY_UV_PERFORMED_DURING_UNLOCK

/**
 * Checks if this [Intent] contains a [Fido2CreateCredentialRequest] related to an ongoing FIDO 2
 * credential creation process.
 */
fun Intent.getFido2CreateCredentialRequestOrNull(): Fido2CreateCredentialRequest? {
    if (isBuildVersionBelow(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)) return null

    val systemRequest = PendingIntentHandler.retrieveProviderCreateCredentialRequest(this)
        ?: return null

    val userId = getStringExtra(EXTRA_KEY_USER_ID)
        ?: return null

    // Extract the OS biometric prompt result from the request data because it is not included in
    // the bundle returned by `ProviderCreateCredentialRequest.asBundle()`.
    val isUserPreVerified = systemRequest
        .biometricPromptResult
        ?.isSuccessful
        ?: getBooleanExtra(EXTRA_KEY_UV_PERFORMED_DURING_UNLOCK, false)

    return Fido2CreateCredentialRequest(
        userId = userId,
        isUserPreVerified = isUserPreVerified,
        requestData = ProviderCreateCredentialRequest.asBundle(systemRequest),
    )
}

/**
 * Checks if this [Intent] contains a [Fido2CredentialAssertionRequest] related to an ongoing FIDO 2
 * credential authentication process.
 */
fun Intent.getFido2AssertionRequestOrNull(): Fido2CredentialAssertionRequest? {
    if (isBuildVersionBelow(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)) return null

    val systemRequest = PendingIntentHandler
        .retrieveProviderGetCredentialRequest(this)
        ?: return null

    val credentialId = getStringExtra(EXTRA_KEY_CREDENTIAL_ID)
        ?: return null

    val cipherId = getStringExtra(EXTRA_KEY_CIPHER_ID)
        ?: return null

    val userId: String = getStringExtra(EXTRA_KEY_USER_ID)
        ?: return null

    // Extract the OS biometric prompt result from the request data because it is not included in
    // the bundle returned by `ProviderGetCredentialRequest.asBundle()`.
    val isUserPreVerified = systemRequest
        .biometricPromptResult
        ?.isSuccessful
        ?: getBooleanExtra(EXTRA_KEY_UV_PERFORMED_DURING_UNLOCK, false)

    return Fido2CredentialAssertionRequest(
        userId = userId,
        cipherId = cipherId,
        credentialId = credentialId,
        isUserPreVerified = isUserPreVerified,
        requestData = ProviderGetCredentialRequest.asBundle(systemRequest),
    )
}

/**
 * Checks if this [Intent] contains a [Fido2GetCredentialsRequest] related to an ongoing FIDO 2
 * credential lookup process.
 */
fun Intent.getFido2GetCredentialsRequestOrNull(): Fido2GetCredentialsRequest? {
    if (isBuildVersionBelow(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)) return null

    val systemRequest = PendingIntentHandler
        .retrieveBeginGetCredentialRequest(this)
        ?: return null

    val userId: String = getStringExtra(EXTRA_KEY_USER_ID)
        ?: return null

    return Fido2GetCredentialsRequest(
        userId = userId,
        requestData = BeginGetCredentialRequest.asBundle(systemRequest),
    )
}
