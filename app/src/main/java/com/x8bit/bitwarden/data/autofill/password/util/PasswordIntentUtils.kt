package com.x8bit.bitwarden.data.autofill.password.util

import android.content.Intent
import android.os.Build
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.provider.PendingIntentHandler
import com.x8bit.bitwarden.data.autofill.password.model.PasswordCredentialRequest
import com.x8bit.bitwarden.data.autofill.password.model.PasswordGetCredentialsRequest
import com.x8bit.bitwarden.data.platform.util.isBuildVersionBelow
import com.x8bit.bitwarden.ui.platform.manager.intent.EXTRA_KEY_CIPHER_ID
import com.x8bit.bitwarden.ui.platform.manager.intent.EXTRA_KEY_PASSWORD_CREDENTIAL_ID
import com.x8bit.bitwarden.ui.platform.manager.intent.EXTRA_KEY_USER_ID

/**
 * Checks if this [Intent] contains a [PasswordCredentialRequest] related to an ongoing Password
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
        userName = createPublicKeyRequest.id,
        password = createPublicKeyRequest.password,
        packageName = systemRequest.callingAppInfo.packageName,
        signingInfo = systemRequest.callingAppInfo.signingInfo,
        origin = systemRequest.callingAppInfo.origin,
    )
}

/**
 * Checks if this [Intent] contains a [PasswordGetCredentialsRequest] related to an ongoing Password
 * credential lookup process.
 */
fun Intent.getPasswordGetCredentialsRequestOrNull(): PasswordGetCredentialsRequest? {
    if (isBuildVersionBelow(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)) return null

    val systemRequest = PendingIntentHandler
        .retrieveProviderGetCredentialRequest(this)
        ?: return null

    val option: GetPasswordOption = systemRequest
        .credentialOptions
        .firstNotNullOfOrNull { it as? GetPasswordOption }
        ?: return null

    val callingAppInfo = systemRequest
        .callingAppInfo

    val cipherId = getStringExtra(EXTRA_KEY_CIPHER_ID)
        ?: return null

    val userId: String = getStringExtra(EXTRA_KEY_USER_ID)
        ?: return null

    val id: String = getStringExtra(EXTRA_KEY_PASSWORD_CREDENTIAL_ID)
        ?: return null

    return PasswordGetCredentialsRequest(
        candidateQueryData = option.candidateQueryData,
        id = id,
        userId = userId,
        cipherId = cipherId,
        allowedUserIds = option.allowedUserIds,
        packageName = callingAppInfo.packageName,
        signingInfo = callingAppInfo.signingInfo,
        origin = callingAppInfo.origin,
    )
}
