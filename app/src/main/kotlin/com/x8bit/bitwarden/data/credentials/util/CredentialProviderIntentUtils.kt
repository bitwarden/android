package com.x8bit.bitwarden.data.credentials.util

import android.content.Intent
import android.os.Build
import androidx.credentials.CredentialManager
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.provider.ProviderCreateCredentialRequest
import androidx.credentials.provider.ProviderGetCredentialRequest
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.x8bit.bitwarden.data.credentials.manager.EXTRA_KEY_CIPHER_ID
import com.x8bit.bitwarden.data.credentials.manager.EXTRA_KEY_CREDENTIAL_ID
import com.x8bit.bitwarden.data.credentials.manager.EXTRA_KEY_USER_ID
import com.x8bit.bitwarden.data.credentials.manager.EXTRA_KEY_UV_PERFORMED_DURING_UNLOCK
import com.x8bit.bitwarden.data.credentials.model.CreateCredentialRequest
import com.x8bit.bitwarden.data.credentials.model.Fido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.credentials.model.GetCredentialsRequest
import com.x8bit.bitwarden.data.credentials.model.ProviderGetPasswordCredentialRequest

/**
 * Checks if this [Intent] contains a [CreateCredentialRequest] related to an ongoing
 * [CredentialManager] creation process.
 */
fun Intent.getCreateCredentialRequestOrNull(): CreateCredentialRequest? {
    if (!isBuildVersionAtLeast(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)) return null

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

    return CreateCredentialRequest(
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
    if (!isBuildVersionAtLeast(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)) return null

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
 * Checks if this [Intent] contains a [ProviderGetPasswordCredentialRequest] related to an
 * ongoing password credential GetPassword process.
 */
fun Intent.getProviderGetPasswordRequestOrNull(): ProviderGetPasswordCredentialRequest? {
    if (!isBuildVersionAtLeast(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)) return null

    val systemRequest = PendingIntentHandler
        .retrieveProviderGetCredentialRequest(this)
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

    return ProviderGetPasswordCredentialRequest(
        userId = userId,
        cipherId = cipherId,
        isUserPreVerified = isUserPreVerified,
        requestData = ProviderGetCredentialRequest.asBundle(systemRequest),
    )
}

/**
 * Checks if this [Intent] contains a [GetCredentialsRequest] related to an ongoing
 * [CredentialManager] credential lookup process.
 */
fun Intent.getGetCredentialsRequestOrNull(): GetCredentialsRequest? {
    if (!isBuildVersionAtLeast(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)) return null

    val systemRequest = PendingIntentHandler
        .retrieveBeginGetCredentialRequest(this)
        ?: return null

    val userId: String = getStringExtra(EXTRA_KEY_USER_ID)
        ?: return null

    return GetCredentialsRequest(
        userId = userId,
        requestData = BeginGetCredentialRequest.asBundle(systemRequest),
    )
}
