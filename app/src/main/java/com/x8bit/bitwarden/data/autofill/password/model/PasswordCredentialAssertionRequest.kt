package com.x8bit.bitwarden.data.autofill.password.model

import android.content.pm.SigningInfo
import android.os.Bundle
import android.os.Parcelable
import androidx.credentials.provider.CallingAppInfo
import kotlinx.parcelize.Parcelize

/**
 * Models a FIDO 2 credential authentication request parsed from the launching intent.
 */
@Parcelize
data class PasswordCredentialAssertionRequest(
    val candidateQueryData: Bundle,
    val id: String,
    val userId: String,
    val cipherId: String,
    val allowedUserIds: Set<String>,
    val packageName: String,
    val signingInfo: SigningInfo,
    val origin: String?,
) : Parcelable {
    val callingAppInfo: CallingAppInfo
        get() = CallingAppInfo(packageName, signingInfo, origin)
}
