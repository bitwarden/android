package com.x8bit.bitwarden.data.autofill.fido2.model

/**
 * Models the result of validating the origin of a FIDO2 request.
 */
sealed class Fido2ValidateOriginResult {

    /**
     * Represents a successful origin validation.
     */
    data object Success : Fido2ValidateOriginResult()

    /**
     * Represents a validation error.
     */
    sealed class Error : Fido2ValidateOriginResult() {

        /**
         * Indicates the digital asset links file could not be located.
         */
        data object AssetLinkNotFound : Error()

        /**
         * Indicates the application package name was not found in the digital asset links file.
         */
        data object ApplicationNotFound : Error()

        /**
         * Indicates the application fingerprint was not found the digital asset links file.
         */
        data object ApplicationNotVerified : Error()

        /**
         * Indicates the calling application is privileged but its package name is not found within
         * the privileged app allow list.
         */
        data object PrivilegedAppNotAllowed : Error()

        /**
         * Indicates the calling app is privileged but but no matching signing certificate signature
         * is present in the allow list.
         */
        data object PrivilegedAppSignatureNotFound : Error()

        /**
         * Indicates passkeys are not supported for the requesting application.
         */
        data object PasskeyNotSupportedForApp : Error()

        /**
         * Indicates an unknown error was encountered while validating the origin.
         */
        data object Unknown : Error()
    }
}
