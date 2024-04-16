package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Models result of requesting a one-time passcode.
 */
sealed class RequestOtpResult {

    /**
     * Represents a successful send of the one-time passcode.
     */
    data object Success : RequestOtpResult()

    /**
     * Represents a failure to send the one-time passcode.
     */
    data class Error(val message: String?) : RequestOtpResult()
}
