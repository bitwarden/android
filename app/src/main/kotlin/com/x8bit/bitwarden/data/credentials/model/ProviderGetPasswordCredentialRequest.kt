package com.x8bit.bitwarden.data.credentials.model

import android.os.Bundle
import android.os.Parcelable
import androidx.credentials.provider.ProviderGetCredentialRequest
import kotlinx.parcelize.Parcelize

/**
 * A wrapper around [ProviderGetCredentialRequest] that includes additional information needed to
 * fulfill the request.
 *
 * @param userId The ID of the user that owns the credential being requested.
 * @param cipherId The ID of the cipher containing the password to be retrieved.
 * @param isUserVerified Whether the user has been verified prior to this request.
 * @param requestData The original request data from the system.
 */
@Parcelize
data class ProviderGetPasswordCredentialRequest(
    val userId: String,
    val cipherId: String,
    val isUserVerified: Boolean,
    val requestData: Bundle,
) : Parcelable
