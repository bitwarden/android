package com.x8bit.bitwarden.ui.vault.feature.util

import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2RegisterCredentialResult
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText

/**
 * Extension property to convert a [Fido2RegisterCredentialResult.Error] into a [Text] for display
 * in the UI.
 */
val Fido2RegisterCredentialResult.Error.errorMessage: Text
    get() = when (this) {
        Fido2RegisterCredentialResult.Error.Internal -> {
            R.string.generic_error_message
        }

        Fido2RegisterCredentialResult.Error.InvalidSignature -> {
            R.string.passkey_operation_failed_because_app_signature_is_invalid
        }

        Fido2RegisterCredentialResult.Error.MissingHostUrl -> {
            R.string.passkey_operation_failed_because_host_url_is_not_present_in_request
        }

        Fido2RegisterCredentialResult.Error.MissingRpId -> {
            R.string.passkey_operation_failed_because_relying_party_cannot_be_identified
        }

        Fido2RegisterCredentialResult.Error.InvalidCipherSelection -> {
            R.string.passkey_operation_failed_because_the_selected_item_does_not_exist
        }

        Fido2RegisterCredentialResult.Error.DigitalAssetLinkApplicationNotFound -> {
            R.string.passkey_operation_failed_because_app_not_found_in_asset_links
        }

        Fido2RegisterCredentialResult.Error.DigitalAssetLinkFingerprintMismatch -> {
            R.string.passkey_operation_failed_because_app_could_not_be_verified
        }

        Fido2RegisterCredentialResult.Error.DigitalAssetLinkNotFound -> {
            R.string.passkey_operation_failed_because_app_not_found_in_asset_links
        }

        Fido2RegisterCredentialResult.Error.PasskeyNotSupportedForApp -> {
            R.string.passkeys_not_supported_for_this_app
        }
    }
        .asText()
