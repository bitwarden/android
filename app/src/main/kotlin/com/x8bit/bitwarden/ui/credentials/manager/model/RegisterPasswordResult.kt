package com.x8bit.bitwarden.ui.credentials.manager.model

import com.bitwarden.ui.util.Text

/**
 * Represents the result of a Password registration attempt.
 */
sealed class RegisterPasswordResult {
    /**
     * Indicates that the registration was successful.
     */
    data object Success : RegisterPasswordResult()

    /**
     * Indicates that an error occurred during registration.
     */
    data class Error(val message: Text) : RegisterPasswordResult()

    /**
     * Indicates that the registration was cancelled by the user.
     */
    data object Cancelled : RegisterPasswordResult()
}
