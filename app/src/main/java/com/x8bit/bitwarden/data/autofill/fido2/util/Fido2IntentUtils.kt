package com.x8bit.bitwarden.data.autofill.fido2.util

import android.content.Intent
import android.os.Build
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.PendingIntentHandler
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CreateCredentialRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2GetCredentialsRequest
import com.x8bit.bitwarden.data.platform.util.isBuildVersionBelow
import com.x8bit.bitwarden.ui.platform.manager.intent.EXTRA_KEY_CIPHER_ID
import com.x8bit.bitwarden.ui.platform.manager.intent.EXTRA_KEY_CREDENTIAL_ID
import com.x8bit.bitwarden.ui.platform.manager.intent.EXTRA_KEY_USER_ID

/**
 * Checks if this [Intent] contains a [Fido2CreateCredentialRequest] related to an ongoing FIDO 2
 * credential creation process.
 */
fun Intent.getFido2CredentialRequestOrNull(): Fido2CreateCredentialRequest? {
    if (isBuildVersionBelow(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)) return null

    val systemRequest = PendingIntentHandler
        .retrieveProviderCreateCredentialRequest(this)
        ?: return null

    val createPublicKeyRequest = systemRequest
        .callingRequest
        as? CreatePublicKeyCredentialRequest
        ?: return null

    val userId = getStringExtra(EXTRA_KEY_USER_ID)
        ?: return null

    return Fido2CreateCredentialRequest(
        userId = userId,
        requestJson = createPublicKeyRequest.requestJson,
        packageName = systemRequest.callingAppInfo.packageName,
        signingInfo = systemRequest.callingAppInfo.signingInfo,
        origin = systemRequest.callingAppInfo.origin,
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

    val option: GetPublicKeyCredentialOption = systemRequest
        .credentialOptions
        .firstNotNullOfOrNull { it as? GetPublicKeyCredentialOption }
        ?: return null

    val credentialId = getStringExtra(EXTRA_KEY_CREDENTIAL_ID)
        ?: return null

    val cipherId = getStringExtra(EXTRA_KEY_CIPHER_ID)
        ?: return null

    val userId: String = getStringExtra(EXTRA_KEY_USER_ID)
        ?: return null

    return Fido2CredentialAssertionRequest(
        userId = userId,
        cipherId = cipherId,
        credentialId = credentialId,
        requestJson = option.requestJson,
        clientDataHash = option.clientDataHash,
        packageName = systemRequest.callingAppInfo.packageName,
        signingInfo = systemRequest.callingAppInfo.signingInfo,
        origin = systemRequest.callingAppInfo.origin,
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

    val option: BeginGetPublicKeyCredentialOption = systemRequest
        .beginGetCredentialOptions
        .firstNotNullOfOrNull { it as? BeginGetPublicKeyCredentialOption }
        ?: return null

    val callingAppInfo = systemRequest
        .callingAppInfo
        ?: return null

    val userId: String = getStringExtra(EXTRA_KEY_USER_ID)
        ?: return null

    return Fido2GetCredentialsRequest(
        candidateQueryData = option.candidateQueryData,
        id = option.id,
        userId = userId,
        requestJson = option.requestJson,
        clientDataHash = option.clientDataHash,
        packageName = callingAppInfo.packageName,
        signingInfo = callingAppInfo.signingInfo,
        origin = callingAppInfo.origin,
    )
}
