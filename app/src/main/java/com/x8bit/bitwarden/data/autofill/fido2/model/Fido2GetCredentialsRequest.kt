package com.x8bit.bitwarden.data.autofill.fido2.model

import android.os.Bundle
import android.os.Parcelable
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.CallingAppInfo
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * Models a FIDO 2 request to retrieve FIDO credentials parsed from the launching intent.
 *
 * @param userId The ID of the user's vault to search.
 * @param requestData Provider request data in the form of a [Bundle].
 */
@Parcelize
data class Fido2GetCredentialsRequest(
    val userId: String,
    private val requestData: Bundle,
) : Parcelable {
    /**
     * The [BeginGetCredentialRequest] from the [requestData], or null if the [requestData] does not
     * contain a [BeginGetCredentialRequest].
     */
    @IgnoredOnParcel
    val providerRequest: BeginGetCredentialRequest? by lazy {
        BeginGetCredentialRequest.fromBundle(requestData)
    }

    /**
     * The first [BeginGetPublicKeyCredentialOption] of the [providerRequest], or null if the
     * [providerRequest] is not a [BeginGetCredentialRequest] or does not contain a
     * [BeginGetPublicKeyCredentialOption].
     */
    @IgnoredOnParcel
    val beginGetPublicKeyCredentialOption: BeginGetPublicKeyCredentialOption? by lazy {
        providerRequest
            ?.beginGetCredentialOptions
            ?.filterIsInstance<BeginGetPublicKeyCredentialOption>()
            ?.firstOrNull()
    }

    /**
     * The [CallingAppInfo] of the [providerRequest], or null if the [providerRequest] is not a
     * [BeginGetCredentialRequest].
     */
    @IgnoredOnParcel
    val callingAppInfo: CallingAppInfo? by lazy { providerRequest?.callingAppInfo }

    /**
     * The first [BeginGetPublicKeyCredentialOption] of the [providerRequest], or null if the
     * [providerRequest] does not contain a [BeginGetPublicKeyCredentialOption].
     */
    @IgnoredOnParcel
    val option: BeginGetPublicKeyCredentialOption? by lazy {
        providerRequest?.beginGetCredentialOptions
            ?.firstNotNullOfOrNull {
                it as? BeginGetPublicKeyCredentialOption
            }
    }
}
