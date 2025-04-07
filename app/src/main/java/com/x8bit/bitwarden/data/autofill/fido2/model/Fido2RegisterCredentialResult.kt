package com.x8bit.bitwarden.data.autofill.fido2.model

import com.bitwarden.core.ui.util.Text

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
    data class Error(val message: Text) : Fido2RegisterCredentialResult()

    /**
     * Indicates the user cancelled the request.
     */
    data object Cancelled : Fido2RegisterCredentialResult()
}
