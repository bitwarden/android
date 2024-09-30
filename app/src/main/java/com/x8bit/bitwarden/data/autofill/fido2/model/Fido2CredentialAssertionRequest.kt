package com.x8bit.bitwarden.data.autofill.fido2.model

import android.content.pm.SigningInfo
import android.os.Parcelable
import androidx.credentials.provider.CallingAppInfo
import kotlinx.parcelize.Parcelize

/**
 * Models a FIDO 2 credential authentication request parsed from the launching intent.
 */
@Parcelize
data class Fido2CredentialAssertionRequest(
    val userId: String,
    val cipherId: String?,
    val credentialId: String?,
    val requestJson: String,
    val clientDataHash: ByteArray?,
    val packageName: String,
    val signingInfo: SigningInfo,
    val origin: String?,
) : Parcelable {
    val callingAppInfo: CallingAppInfo
        get() = CallingAppInfo(packageName, signingInfo, origin)
}
