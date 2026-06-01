package com.x8bit.bitwarden.ui.vault.feature.itemlisting.util

import androidx.annotation.StringRes
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.data.credentials.model.Fido2RegisterCredentialResult

/**
 * Returns the string resource ID corresponding to the error message for the given
 * [Fido2RegisterCredentialResult.Error].
 */
val Fido2RegisterCredentialResult.Error.messageResourceId: Int
    @StringRes
    get() = when (this) {
        Fido2RegisterCredentialResult.Error.InternalError -> {
            BitwardenString.passkey_registration_failed_due_to_an_internal_error
        }

        Fido2RegisterCredentialResult.Error.InvalidAppSignature -> {
            BitwardenString.passkey_operation_failed_because_app_signature_is_invalid
        }

        Fido2RegisterCredentialResult.Error.MissingHostUrl -> {
            BitwardenString.passkey_operation_failed_because_host_url_is_not_present_in_request
        }
    }
