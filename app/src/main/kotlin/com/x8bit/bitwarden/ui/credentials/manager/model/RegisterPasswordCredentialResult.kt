package com.x8bit.bitwarden.ui.credentials.manager.model

import com.bitwarden.ui.util.Text

/**
 * Represents the result of a Password credential registration attempt.
 */
sealed class RegisterPasswordCredentialResult {
    /**
     * Indicates that the registration was successful.
     */
    data object Success : RegisterPasswordCredentialResult()

    /**
     * Indicates that an error occurred during registration.
     */
    data class Error(val message: Text) : RegisterPasswordCredentialResult()

    /**
     * Indicates that the registration was cancelled by the user.
     */
    data object Cancelled : RegisterPasswordCredentialResult()
}
