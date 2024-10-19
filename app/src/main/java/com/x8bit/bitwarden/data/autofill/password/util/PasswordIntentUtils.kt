package com.x8bit.bitwarden.data.autofill.password.util

import android.content.Intent
import android.os.Build
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.PendingIntentHandler
import com.x8bit.bitwarden.data.autofill.password.model.PasswordCredentialAssertionRequest
import com.x8bit.bitwarden.data.autofill.password.model.PasswordCredentialRequest
import com.x8bit.bitwarden.data.autofill.password.model.PasswordGetCredentialsRequest
import com.x8bit.bitwarden.data.platform.util.isBuildVersionBelow
import com.x8bit.bitwarden.ui.platform.manager.intent.EXTRA_KEY_CIPHER_ID
import com.x8bit.bitwarden.ui.platform.manager.intent.EXTRA_KEY_CREDENTIAL_ID
import com.x8bit.bitwarden.ui.platform.manager.intent.EXTRA_KEY_USER_ID

/**
 * Checks if this [Intent] contains a [PasswordCredentialRequest] related to an ongoing FIDO 2
 * credential creation process.
 */
fun Intent.getPasswordCredentialRequestOrNull(): PasswordCredentialRequest? {
    if (isBuildVersionBelow(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)) return null

    val systemRequest = PendingIntentHandler
        .retrieveProviderCreateCredentialRequest(this)
        ?: return null

    val createPublicKeyRequest = systemRequest
        .callingRequest
        as? CreatePasswordRequest
        ?: return null

    val userId = getStringExtra(EXTRA_KEY_USER_ID)
        ?: return null

    return PasswordCredentialRequest(
        userId = userId,
        password = createPublicKeyRequest.password,
        packageName = systemRequest.callingAppInfo.packageName,
        signingInfo = systemRequest.callingAppInfo.signingInfo,
        origin = systemRequest.callingAppInfo.origin,
    )
}

/**
 * Checks if this [Intent] contains a [PasswordCredentialAssertionRequest] related to an ongoing Password
 * credential authentication process.
 */
fun Intent.getPasswordAssertionRequestOrNull(): PasswordCredentialAssertionRequest? {
    if (isBuildVersionBelow(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)) return null

    val systemRequest = PendingIntentHandler
        .retrieveProviderGetCredentialRequest(this)
        ?: return null

    val option: GetPasswordOption = systemRequest
        .credentialOptions
        .firstNotNullOfOrNull { it as? GetPasswordOption }
        ?: return null

    val credentialId = getStringExtra(EXTRA_KEY_CREDENTIAL_ID)
        ?: return null

    val cipherId = getStringExtra(EXTRA_KEY_CIPHER_ID)
        ?: return null

    val userId: String = getStringExtra(EXTRA_KEY_USER_ID)
        ?: return null

    return PasswordCredentialAssertionRequest(
        userId = userId,
        cipherId = cipherId,
        credentialId = credentialId,
        allowedUserIds = option.allowedUserIds,
        isAutoSelectAllowed = option.isAutoSelectAllowed,
        packageName = systemRequest.callingAppInfo.packageName,
        signingInfo = systemRequest.callingAppInfo.signingInfo,
        origin = systemRequest.callingAppInfo.origin,
    )
}

/**
 * Checks if this [Intent] contains a [PasswordGetCredentialsRequest] related to an ongoing FIDO 2
 * credential lookup process.
 */
fun Intent.getPasswordGetCredentialsRequestOrNull(): PasswordGetCredentialsRequest? {
    if (isBuildVersionBelow(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)) return null

    val systemRequest = PendingIntentHandler
        .retrieveBeginGetCredentialRequest(this)
        ?: return null

    val option: BeginGetPasswordOption = systemRequest
        .beginGetCredentialOptions
        .firstNotNullOfOrNull { it as? BeginGetPasswordOption }
        ?: return null

    val callingAppInfo = systemRequest
        .callingAppInfo
        ?: return null

    val userId: String = getStringExtra(EXTRA_KEY_USER_ID)
        ?: return null

    return PasswordGetCredentialsRequest(
        candidateQueryData = option.candidateQueryData,
        id = option.id,
        userId = userId,
        allowedUserIds = option.allowedUserIds,
        packageName = callingAppInfo.packageName,
        signingInfo = callingAppInfo.signingInfo,
        origin = callingAppInfo.origin,
    )
}
