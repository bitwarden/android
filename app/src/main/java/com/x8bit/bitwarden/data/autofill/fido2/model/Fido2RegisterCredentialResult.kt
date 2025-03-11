package com.x8bit.bitwarden.data.autofill.fido2.model

/**
 * Models the data returned from creating a FIDO 2 credential.
 */
sealed class Fido2RegisterCredentialResult {

    /**
     * Indicates the credential has been successfully registered.
     */
    data class Success(
        val responseJson: String,
    ) : Fido2RegisterCredentialResult()

    /**
     * Indicates the calling application is acting as a privileged app but is not trusted.
     */
    data class PrivilegedAppNotTrusted(
        val selectedCipherId: String?,
    ) : Fido2RegisterCredentialResult()

    /**
     * Indicates there was an error and the credential was not registered.
     */
    sealed class Error : Fido2RegisterCredentialResult() {
        /**
         * Indicates that the Relying Party ID was missing from the registration request.
         */
        data object MissingRpId : Error()

        /**
         * Indicates that the selected cipher was invalid.
         */
        data object InvalidCipherSelection : Error()

        /**
         * Indicates that the calling application's signing certificate signature is invalid.
         */
        data object InvalidSignature : Error()

        /**
         * Indicates that the host URL was missing or could not be derived from the request.
         */
        data object MissingHostUrl : Error()

        /**
         * Indicates an internal error occurred.
         */
        data object Internal : Error()

        /**
         * Indicates that passkeys are not supported for the calling app.
         */
        data object PasskeyNotSupportedForApp : Error()

        /**
         * Indicates that the Digital Asset Link file does not include the calling application's
         * package name.
         */
        data object DigitalAssetLinkApplicationNotFound : Error()

        /**
         * Indicates that the Digital Asset Link file fingerprints did not match the calling
         * application's signing certificate fingerprint.
         */
        data object DigitalAssetLinkFingerprintMismatch : Error()

        /**
         * Indicates that the Digital Asset Link file could not be found.
         */
        data object DigitalAssetLinkNotFound : Error()
    }

    /**
     * Indicates the user cancelled the request.
     */
    data object Cancelled : Fido2RegisterCredentialResult()
}
