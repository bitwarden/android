package com.x8bit.bitwarden.data.credentials.model

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
     * Indicates there was an error and the credential was not registered.
     */
    sealed class Error : Fido2RegisterCredentialResult() {

        /**
         * Indicates the host URL was missing from the request.
         */
        data object MissingHostUrl : Error()

        /**
         * Indicates the app signature was invalid.
         */
        data object InvalidAppSignature : Error()

        /**
         * Indicates an internal error occurred.
         */
        data object InternalError : Error()
    }
}
