package com.x8bit.bitwarden.data.autofill.fido2.model

import android.os.Bundle
import android.os.Parcelable
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.ProviderCreateCredentialRequest
import com.x8bit.bitwarden.ui.platform.base.util.toHostOrPathOrNull
import kotlinx.parcelize.Parcelize

/**
 * Represents raw data from the a user deciding to create a passkey in their vault via the
 * credential manager framework.
 *
 * @property userId The ID of the user creating the passkey.
 * @property requestData Provider request data in the form of a [Bundle].
 */
@Parcelize
data class Fido2CreateCredentialRequest(
    val userId: String,
    val requestData: Bundle,
) : Parcelable {

    /**
     * The [ProviderCreateCredentialRequest] from the [requestData].
     */
    val providerRequest: ProviderCreateCredentialRequest
        get() = ProviderCreateCredentialRequest.fromBundle(requestData)

    /**
     * The [CallingAppInfo] of the [providerRequest].
     */
    val callingAppInfo: CallingAppInfo
        get() = providerRequest.callingAppInfo

    /**
     * The [CreatePublicKeyCredentialRequest] of the [providerRequest], or null if the calling
     * request is not a [CreatePublicKeyCredentialRequest].
     */
    val createPublicKeyCredentialRequest: CreatePublicKeyCredentialRequest?
        get() = providerRequest.callingRequest as? CreatePublicKeyCredentialRequest

    /**
     * The [requestJson] of the [createPublicKeyCredentialRequest], or null if the calling request
     * is not a [CreatePublicKeyCredentialRequest].
     */
    val requestJson: String?
        get() = createPublicKeyCredentialRequest?.requestJson

    /**
     * Returns the ID of the relying party, or null if the relying party cannot be identified.
     */
    val relyingPartyIdOrNull: String?
        get() = if (callingAppInfo.isOriginPopulated()) {
            providerRequest.callingRequest.origin?.toHostOrPathOrNull()
        } else {
            callingAppInfo.packageName
        }
}
