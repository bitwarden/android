package com.x8bit.bitwarden.ui.vault.feature.itemlisting.util

import androidx.annotation.StringRes
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.data.credentials.model.ValidateOriginResult

/**
 * Returns the string resource ID corresponding to the error message for the given
 * [ValidateOriginResult.Error].
 */
val ValidateOriginResult.Error.messageResourceId: Int
    @StringRes
    get() = when (this) {
        ValidateOriginResult.Error.AssetLinkNotFound -> {
            BitwardenString.passkey_operation_failed_because_of_missing_asset_links
        }

        ValidateOriginResult.Error.PasskeyNotSupportedForApp -> {
            BitwardenString.passkeys_not_supported_for_this_app
        }

        ValidateOriginResult.Error.PrivilegedAppNotAllowed -> {
            BitwardenString.passkey_operation_failed_because_browser_is_not_privileged
        }

        ValidateOriginResult.Error.PrivilegedAppSignatureNotFound -> {
            BitwardenString.passkey_operation_failed_because_browser_signature_does_not_match
        }

        ValidateOriginResult.Error.Unknown -> {
            BitwardenString.generic_error_message
        }
    }
