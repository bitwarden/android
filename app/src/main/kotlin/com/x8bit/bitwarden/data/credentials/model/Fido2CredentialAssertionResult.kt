package com.x8bit.bitwarden.data.credentials.model

/**
 * Represents possible outcomes of a FIDO 2 credential assertion request.
 */
sealed class Fido2CredentialAssertionResult {

    /**
     * Indicates the assertion request completed and [responseJson] was successfully generated.
     */
    data class Success(val responseJson: String) : Fido2CredentialAssertionResult()

    /**
     * Indicates there was an error and the assertion was not successful.
     */
    sealed class Error : Fido2CredentialAssertionResult() {
        /**
         * Indicates the relying party ID was missing from the request.
         */
        data object MissingRpId : Error()

        /**
         * Indicates the host URL was missing from the request.
         */
        data object MissingHostUrl : Error()

        /**
         * Indicates an internal error occurred.
         */
        data object InternalError : Error()
    }
}
