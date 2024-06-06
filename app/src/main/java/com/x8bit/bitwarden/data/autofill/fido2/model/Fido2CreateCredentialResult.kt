package com.x8bit.bitwarden.data.autofill.fido2.model

import androidx.credentials.exceptions.CreateCredentialException

/**
 * Models the data returned from creating a FIDO 2 credential.
 */
sealed class Fido2CreateCredentialResult {

    /**
     * Models a successful response for creating a credential.
     */
    data class Success(
        val registrationResponse: String,
    ) : Fido2CreateCredentialResult()

    /**
     * Models an error response for creating a credential.
     */
    data class Error(
        val exception: CreateCredentialException,
    ) : Fido2CreateCredentialResult()
}
