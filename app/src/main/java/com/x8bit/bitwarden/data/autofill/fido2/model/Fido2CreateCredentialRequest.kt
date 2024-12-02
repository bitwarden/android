package com.x8bit.bitwarden.data.autofill.fido2.model

import android.content.pm.SigningInfo
import android.os.Parcelable
import androidx.credentials.provider.CallingAppInfo
import kotlinx.parcelize.Parcelize

/**
 * Represents raw data from the a user deciding to create a passkey in their vault via the
 * credential manager framework.
 *
 * @property userId The user under which the passkey should be saved.
 * @property requestJson JSON payload containing the RP request.
 * @property callingAppInfo Information about the application that initiated the request.
 */
@Parcelize
data class Fido2CreateCredentialRequest(
    val userId: String,
    val requestJson: String,
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
