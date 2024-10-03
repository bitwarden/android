package com.x8bit.bitwarden.data.autofill.fido2.model

import android.content.pm.SigningInfo
import android.os.Bundle
import android.os.Parcelable
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.CallingAppInfo
import kotlinx.parcelize.Parcelize

/**
 * Models a FIDO 2 request to retrieve FIDO credentials parsed from the launching intent.
 */
@Parcelize
data class Fido2GetCredentialsRequest(
    val candidateQueryData: Bundle,
    val id: String,
    val userId: String,
    val requestJson: String,
    val clientDataHash: ByteArray? = null,
    val packageName: String,
    val signingInfo: SigningInfo,
    val origin: String?,
) : Parcelable {
    val callingAppInfo: CallingAppInfo
        get() = CallingAppInfo(packageName, signingInfo, origin)

    val option: BeginGetPublicKeyCredentialOption
        get() = BeginGetPublicKeyCredentialOption(
            candidateQueryData,
            id,
            requestJson,
            clientDataHash,
        )
}
