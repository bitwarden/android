package com.x8bit.bitwarden.data.autofill.fido2.model

/**
 * Models the data returned from creating a FIDO 2 credential.
 */
sealed class Fido2CreateCredentialResult {

    /**
     * Indicates the credential has been successfully registered.
     */
    data class Success(
        val registrationResponse: String,
    ) : Fido2CreateCredentialResult()

    /**
     * Indicates there was an error and the credential was not registered.
     */
    data object Error : Fido2CreateCredentialResult()

    /**
     * Indicates the user cancelled the request.
     */
    data object Cancelled : Fido2CreateCredentialResult()
}
