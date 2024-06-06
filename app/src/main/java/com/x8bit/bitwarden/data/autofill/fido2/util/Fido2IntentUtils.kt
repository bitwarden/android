package com.x8bit.bitwarden.data.autofill.fido2.util

import android.content.Intent
import android.os.Build
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.provider.PendingIntentHandler
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialRequest
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.platform.util.isBuildVersionBelow
import com.x8bit.bitwarden.ui.platform.manager.intent.EXTRA_KEY_USER_ID

/**
 * Checks if this [Intent] contains a [Fido2CredentialRequest] related to an ongoing FIDO 2
 * credential creation process.
 */
@Suppress("ReturnCount")
@OmitFromCoverage
fun Intent.getFido2CredentialRequestOrNull(): Fido2CredentialRequest? {
    if (isBuildVersionBelow(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)) return null

    val systemRequest = PendingIntentHandler.retrieveProviderCreateCredentialRequest(this)
        ?: return null

    val createPublicKeyRequest =
        systemRequest.callingRequest as? CreatePublicKeyCredentialRequest
            ?: return null

    val userId = getStringExtra(EXTRA_KEY_USER_ID)
        ?: return null

    return Fido2CredentialRequest(
        userId = userId,
        requestJson = createPublicKeyRequest.requestJson,
        packageName = systemRequest.callingAppInfo.packageName,
        signingInfo = systemRequest.callingAppInfo.signingInfo,
        origin = systemRequest.callingAppInfo.origin,
    )
}
