package com.x8bit.bitwarden.ui.credentials.manager.model

import com.bitwarden.ui.util.Text

/**
 * Represents the result of a credential creation attempt.
 */
sealed class CreateCredentialResult {

    /**
     * Represents a successful credential creation attempt.
     */
    sealed class Success : CreateCredentialResult() {
        /**
         * Indicates that the FIDO2 registration was successful.
         */
        data class Fido2CredentialRegistered(val responseJson: String) : Success()

        /**
         * Indicates that the Password creation was successful.
         */
        data object PasswordCreated : Success()
    }

    /**
     * Indicates that an error occurred during credential creation.
     */
    data class Error(val message: Text) : CreateCredentialResult()

    /**
     * Indicates that credential creation was cancelled by the user.
     */
    data object Cancelled : CreateCredentialResult()
}
