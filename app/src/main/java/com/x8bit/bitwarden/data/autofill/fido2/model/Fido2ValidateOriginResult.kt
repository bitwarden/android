package com.x8bit.bitwarden.data.autofill.fido2.model

import androidx.annotation.StringRes
import com.x8bit.bitwarden.R

/**
 * Models the result of validating the origin of a FIDO2 request.
 */
sealed class Fido2ValidateOriginResult {

    /**
     * Represents a successful origin validation.
     *
     * @param origin The origin of the calling app, or null if the calling app is not privileged.
     */
    data class Success(val origin: String?) : Fido2ValidateOriginResult()

    /**
     * Represents a validation error.
     */
    sealed class Error : Fido2ValidateOriginResult() {
        /**
         * The string resource ID of the error message.
         */
        @get:StringRes
        abstract val messageResId: Int

        /**
         * Indicates the digital asset links file could not be located.
         */
        data object AssetLinkNotFound : Error() {
            override val messageResId =
                R.string.passkey_operation_failed_because_of_missing_asset_links
        }

        /**
         * Indicates the application package name was not found in the digital asset links file.
         */
        data object ApplicationNotFound : Error() {
            override val messageResId =
                R.string.passkey_operation_failed_because_app_not_found_in_asset_links
        }

        /**
         * Indicates the application fingerprint was not found the digital asset links file.
         */
        data object ApplicationFingerprintNotVerified : Error() {
            override val messageResId =
                R.string.passkey_operation_failed_because_app_could_not_be_verified
        }

        /**
         * Indicates the calling application is privileged but its package name is not found within
         * the privileged app allow list.
         */
        data object PrivilegedAppNotAllowed : Error() {
            override val messageResId =
                R.string.passkey_operation_failed_because_browser_is_not_privileged
        }

        /**
         * Indicates the calling app is privileged but but no matching signing certificate signature
         * is present in the allow list.
         */
        data object PrivilegedAppSignatureNotFound : Error() {
            override val messageResId =
                R.string.passkey_operation_failed_because_browser_signature_does_not_match
        }

        /**
         * Indicates passkeys are not supported for the requesting application.
         */
        data object PasskeyNotSupportedForApp : Error() {
            override val messageResId = R.string.passkeys_not_supported_for_this_app
        }

        /**
         * Indicates an unknown error was encountered while validating the origin.
         */
        data object Unknown : Error() {
            override val messageResId = R.string.generic_error_message
        }
    }
}
