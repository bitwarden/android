package com.x8bit.bitwarden.data.credentials.model

import android.os.Bundle
import android.os.Parcelable
import androidx.credentials.CredentialManager
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.CallingAppInfo
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * Models a [CredentialManager] request to retrieve credentials parsed from the launching intent.
 *
 * @param userId The ID of the user's vault to search.
 * @param requestData Provider request data in the form of a [Bundle].
 */
@Parcelize
data class GetCredentialsRequest(
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
     * The [BeginGetPublicKeyCredentialOption]s of the [providerRequest], or an empty list if no
     * public key options are present.
     */
    @IgnoredOnParcel
    val beginGetPublicKeyCredentialOptions: List<BeginGetPublicKeyCredentialOption> by lazy {
        providerRequest
            ?.beginGetCredentialOptions
            ?.filterIsInstance<BeginGetPublicKeyCredentialOption>()
            .orEmpty()
    }

    /**
     * The [BeginGetPasswordOption]s of the [providerRequest], or an empty list if no password
     * options are present.
     */
    @IgnoredOnParcel
    val beginGetPasswordOptions: List<BeginGetPasswordOption> by lazy {
        providerRequest
            ?.beginGetCredentialOptions
            ?.filterIsInstance<BeginGetPasswordOption>()
            .orEmpty()
    }

    /**
     * The [CallingAppInfo] of the [providerRequest], or null if the [providerRequest] is not a
     * [BeginGetCredentialRequest].
     */
    @IgnoredOnParcel
    val callingAppInfo: CallingAppInfo? by lazy { providerRequest?.callingAppInfo }
}
