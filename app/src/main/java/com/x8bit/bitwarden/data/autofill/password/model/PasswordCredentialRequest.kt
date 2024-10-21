package com.x8bit.bitwarden.data.autofill.password.model

import android.content.pm.SigningInfo
import android.os.Parcelable
import androidx.credentials.provider.CallingAppInfo
import kotlinx.parcelize.Parcelize

/**
 * Represents raw data from the a user deciding to create a password in their vault via the
 * credential manager framework.
 *
 * @property userId The user under which the password should be saved.
 * @property userName containing the username from the request.
 * @property password containing the password from the request.
 * @property callingAppInfo Information about the application that initiated the request.
 */
@Parcelize
data class PasswordCredentialRequest(
    val userId: String,
    val userName: String,
    val password: String,
    val packageName: String,
    val signingInfo: SigningInfo,
    val origin: String?,
) : Parcelable {
    val callingAppInfo: CallingAppInfo
        get() = CallingAppInfo(
            packageName = packageName,
            signingInfo = signingInfo,
            origin = origin,
        )
}
