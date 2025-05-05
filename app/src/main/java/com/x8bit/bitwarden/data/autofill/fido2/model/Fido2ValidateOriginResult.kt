package com.x8bit.bitwarden.data.autofill.fido2.model

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
         * Indicates the digital asset links file could not be located.
         */
        data object AssetLinkNotFound : Error()

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
