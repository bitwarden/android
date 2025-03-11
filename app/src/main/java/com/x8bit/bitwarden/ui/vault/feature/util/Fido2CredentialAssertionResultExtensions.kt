package com.x8bit.bitwarden.ui.vault.feature.util

import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionResult
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText

/**
 * Extension property to convert a [Fido2CredentialAssertionResult.Error] into a [Text] for display.
 */
val Fido2CredentialAssertionResult.Error.errorMessage: Text
    get() = when (this) {
        Fido2CredentialAssertionResult.Error.Internal -> {
            R.string.generic_error_message
        }

        Fido2CredentialAssertionResult.Error.InvalidAppSignature -> {
            R.string.passkey_operation_failed_because_app_signature_is_invalid
        }

        Fido2CredentialAssertionResult.Error.InvalidAssetLink -> {
            R.string.passkey_operation_failed_because_of_missing_asset_links
        }

        Fido2CredentialAssertionResult.Error.InvalidRpId -> {
            R.string.passkey_operation_failed_because_relying_party_cannot_be_identified
        }

        Fido2CredentialAssertionResult.Error.MissingHostUrl -> {
            R.string.passkey_operation_failed_because_host_url_is_not_present_in_request
        }

        Fido2CredentialAssertionResult.Error.NotSupported -> {
            R.string.passkeys_not_supported_for_this_app
        }
    }
        .asText()
