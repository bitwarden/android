package com.x8bit.bitwarden.ui.credentials.manager.model

import com.bitwarden.ui.util.Text

/**
 * Represents the result of a credential registration attempt.
 */
sealed class RegisterCredentialResult {
    /**
     * Indicates that the FIDO2 registration was successful.
     */
    data class SuccessFido2(val responseJson: String) : RegisterCredentialResult()

    /**
     * Indicates that the Password registration was successful.
     */
    data object SuccessPassword : RegisterCredentialResult()

    /**
     * Indicates that an error occurred during registration.
     */
    data class Error(val message: Text) : RegisterCredentialResult()

    /**
     * Indicates that the registration was cancelled by the user.
     */
    data object Cancelled : RegisterCredentialResult()
}
