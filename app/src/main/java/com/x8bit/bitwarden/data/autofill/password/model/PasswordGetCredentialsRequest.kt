package com.x8bit.bitwarden.data.autofill.password.model

import android.content.pm.SigningInfo
import android.os.Bundle
import android.os.Parcelable
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.CallingAppInfo
import kotlinx.parcelize.Parcelize

/**
 * Models a FIDO 2 request to retrieve FIDO credentials parsed from the launching intent.
 */
@Parcelize
data class PasswordGetCredentialsRequest(
    val candidateQueryData: Bundle,
    val userId: String,
    val id: String,
    val allowedUserIds: Set<String>,
    val packageName: String,
    val signingInfo: SigningInfo,
    val origin: String?,
) : Parcelable {
    val callingAppInfo: CallingAppInfo
        get() = CallingAppInfo(packageName, signingInfo, origin)

    val option: BeginGetPasswordOption
        get() = BeginGetPasswordOption(
            allowedUserIds,
            candidateQueryData,
            id,
        )
}
