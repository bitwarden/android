package com.x8bit.bitwarden.data.autofill.fido2.model

/**
 * Represents possible outcomes of a FIDO 2 credential assertion request.
 */
sealed class Fido2CredentialAssertionResult {

    /**
     * Indicates the assertion request completed and [responseJson] was successfully generated.
     */
    data class Success(val responseJson: String) : Fido2CredentialAssertionResult()

    /**
     * Indicates that the privileged app is not trusted.
     */
    data object PrivilegedAppNotTrusted : Fido2CredentialAssertionResult()

    /**
     * Indicates there was an error and the assertion was not successful.
     */
    sealed class Error : Fido2CredentialAssertionResult() {
        /**
         * Indicates that the Relying Party ID (RP ID) is invalid.
         */
        data object InvalidRpId : Error()

        /**
         * Indicates that the Asset Link is invalid.
         */
        data object InvalidAssetLink : Error()

        /**
         * Indicates that the requested operation is not supported.
         */
        data object NotSupported : Error()

        /**
         * Indicates that the host URL is missing.
         */
        data object MissingHostUrl : Error()

        /**
         * Indicates that the app signature is invalid.
         */
        data object InvalidAppSignature : Error()

        /**
         * Indicates that an internal error occurred.
         */
        data object Internal : Error()
    }
}
