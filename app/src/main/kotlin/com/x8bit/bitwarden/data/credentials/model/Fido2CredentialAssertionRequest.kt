package com.x8bit.bitwarden.data.credentials.model

import android.os.Bundle
import android.os.Parcelable
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.ProviderGetCredentialRequest
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * Models a FIDO 2 credential authentication request parsed from the launching intent.
 *
 * @property userId ID of the user requesting credential authentication.
 * @property cipherId ID of the cipher to be authenticated against.
 * @property credentialId ID of the credential to authenticate.
 * @property isUserPreVerified Whether the user has already been verified by the OS biometric
 * prompt.
 * @property requestData Provider request data in the form of a [Bundle].
 */
@Parcelize
data class Fido2CredentialAssertionRequest(
    val userId: String,
    val cipherId: String,
    val credentialId: String,
    val isUserPreVerified: Boolean,
    private val requestData: Bundle,
) : Parcelable {

    /**
     * The [ProviderGetCredentialRequest] from the [requestData].
     */
    @IgnoredOnParcel
    val providerRequest: ProviderGetCredentialRequest by lazy {
        ProviderGetCredentialRequest.fromBundle(requestData)
    }

    /**
     * The [CallingAppInfo] from the [providerRequest].
     */
    @IgnoredOnParcel
    val callingAppInfo: CallingAppInfo by lazy { providerRequest.callingAppInfo }

    /**
     * The [GetPublicKeyCredentialOption] from the [providerRequest], or null if one is not found
     * in the request options list.
     */
    @IgnoredOnParcel
    val option: GetPublicKeyCredentialOption? by lazy {
        providerRequest.credentialOptions
            .firstNotNullOfOrNull { it as? GetPublicKeyCredentialOption }
    }
}
