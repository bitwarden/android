package com.x8bit.bitwarden.ui.credentials.manager.model

import com.bitwarden.ui.util.Text

/**
 * Represents the result of a FIDO2 credential registration attempt.
 */
sealed class RegisterFido2CredentialResult {
    /**
     * Indicates that the registration was successful.
     */
    data class Success(val responseJson: String) : RegisterFido2CredentialResult()

    /**
     * Indicates that an error occurred during registration.
     */
    data class Error(val message: Text) : RegisterFido2CredentialResult()

    /**
     * Indicates that the registration was cancelled by the user.
     */
    data object Cancelled : RegisterFido2CredentialResult()
}
