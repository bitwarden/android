package com.x8bit.bitwarden.ui.vault.feature.itemlisting.util

import androidx.annotation.StringRes
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2ValidateOriginResult

/**
 * Returns the string resource ID corresponding to the error message for the given
 * [Fido2ValidateOriginResult.Error].
 */
val Fido2ValidateOriginResult.Error.messageResourceId: Int
    @StringRes
    get() = when (this) {
        Fido2ValidateOriginResult.Error.AssetLinkNotFound -> {
            R.string.passkey_operation_failed_because_of_missing_asset_links
        }

        Fido2ValidateOriginResult.Error.PasskeyNotSupportedForApp -> {
            R.string.passkeys_not_supported_for_this_app
        }

        Fido2ValidateOriginResult.Error.PrivilegedAppNotAllowed -> {
            R.string.passkey_operation_failed_because_browser_is_not_privileged
        }

        Fido2ValidateOriginResult.Error.PrivilegedAppSignatureNotFound -> {
            R.string.passkey_operation_failed_because_browser_signature_does_not_match
        }

        Fido2ValidateOriginResult.Error.Unknown -> {
            R.string.generic_error_message
        }
    }
