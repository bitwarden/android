package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Models result of verifying a one-time passcode.
 */
sealed class VerifyOtpResult {

    /**
     * Represents a successful verification of the one-time passcode.
     */
    data object Verified : VerifyOtpResult()

    /**
     * Represents a failure to verify the one-time passcode.
     */
    data class NotVerified(val errorMessage: String?) : VerifyOtpResult()
}
