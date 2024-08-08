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
     * Indicates there was an error and the assertion was not successful.
     */
    data object Error : Fido2CredentialAssertionResult()
}
