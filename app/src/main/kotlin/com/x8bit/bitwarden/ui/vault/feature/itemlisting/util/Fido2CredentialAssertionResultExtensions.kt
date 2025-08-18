package com.x8bit.bitwarden.ui.vault.feature.itemlisting.util

import androidx.annotation.StringRes
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.data.credentials.model.Fido2CredentialAssertionResult

/**
 * Returns the string resource ID corresponding to the error message for the given
 * [Fido2CredentialAssertionResult.Error].
 */
val Fido2CredentialAssertionResult.Error.messageResourceId: Int
    @StringRes
    get() = when (this) {
        Fido2CredentialAssertionResult.Error.InternalError -> {
            BitwardenString.passkey_registration_failed_due_to_an_internal_error
        }

        Fido2CredentialAssertionResult.Error.MissingHostUrl -> {
            BitwardenString.passkey_operation_failed_because_host_url_is_not_present_in_request
        }

        Fido2CredentialAssertionResult.Error.MissingRpId -> {
            BitwardenString.passkey_operation_failed_because_relying_party_cannot_be_identified
        }
    }
