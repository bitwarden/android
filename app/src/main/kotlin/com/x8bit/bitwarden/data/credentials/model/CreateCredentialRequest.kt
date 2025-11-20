package com.x8bit.bitwarden.data.credentials.model

import android.os.Bundle
import android.os.Parcelable
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.ProviderCreateCredentialRequest
import com.bitwarden.ui.platform.base.util.toHostOrPathOrNull
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * Represents raw data from the a user deciding to create a passkey in their vault via the
 * credential manager framework.
 *
 * @property userId The ID of the user creating the passkey.
 * @property isUserPreVerified Whether the user has already been verified by the OS biometric
 * prompt.
 * @property requestData Provider request data in the form of a [Bundle].
 */
@Parcelize
data class CreateCredentialRequest(
    val userId: String,
    val requestData: Bundle,
    val isUserPreVerified: Boolean,
) : Parcelable {

    /**
     * The [CreateCredentialRequest] from the [requestData].
     */
    @IgnoredOnParcel
    val providerRequest: ProviderCreateCredentialRequest by lazy {
        ProviderCreateCredentialRequest.fromBundle(requestData)
    }

    /**
     * The [CallingAppInfo] of the [providerRequest].
     */
    @IgnoredOnParcel
    val callingAppInfo: CallingAppInfo by lazy { providerRequest.callingAppInfo }

    /**
     * The [CreatePublicKeyCredentialRequest] of the [providerRequest], or null if the calling
     * request is not a [CreatePublicKeyCredentialRequest].
     */
    @IgnoredOnParcel
    val createPublicKeyCredentialRequest: CreatePublicKeyCredentialRequest? by lazy {
        providerRequest.callingRequest as? CreatePublicKeyCredentialRequest
    }

    /**
     * The [CreatePasswordRequest] of the [providerRequest], or null if the calling
     * request is not a [CreatePasswordRequest].
     */
    @IgnoredOnParcel
    val createPasswordCredentialRequest: CreatePasswordRequest? by lazy {
        providerRequest.callingRequest as? CreatePasswordRequest
    }

    /**
     * The [requestJson] of the [createPublicKeyCredentialRequest], or null if the calling request
     * is not a [CreatePublicKeyCredentialRequest].
     */
    @IgnoredOnParcel
    val requestJson: String? by lazy { createPublicKeyCredentialRequest?.requestJson }

    /**
     * Returns the ID of the relying party, or null if the relying party cannot be identified.
     */
    @IgnoredOnParcel
    val relyingPartyIdOrNull: String? by lazy {
        if (callingAppInfo.isOriginPopulated()) {
            providerRequest.callingRequest.origin?.toHostOrPathOrNull()
        } else {
            callingAppInfo.packageName
        }
    }
}
