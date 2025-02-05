package com.x8bit.bitwarden.data.autofill.fido2.model

import android.content.pm.SigningInfo
import android.os.Parcelable
import androidx.credentials.provider.CallingAppInfo
import kotlinx.parcelize.Parcelize

/**
 * Models a FIDO 2 credential authentication request parsed from the launching intent.
 *
 * @param userId The ID of the Bitwarden user to authenticate.
 * @param cipherId The ID of the cipher that contains the passkey to authenticate.
 * @param credentialId The ID of the credential to authenticate.
 * @param requestJson The JSON representation of the FIDO 2 request.
 * @param clientDataHash The hash of the client data.
 * @param packageName The package name of the calling app.
 * @param signingInfo The signing info of the calling app.
 * @param origin The origin of the calling app. Only populated if the calling application is a
 * privileged application. I.e., a web browser.
 * @param isUserVerified Whether the user has been verified prior to receiving this request. Only
 * populated if device biometric verification was performed. If null, the application is responsible
 * for prompting user verification when it is deemed necessary.
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
    val isUserVerified: Boolean?,
) : Parcelable {
    val callingAppInfo: CallingAppInfo
        get() = CallingAppInfo(packageName, signingInfo, origin)
}
