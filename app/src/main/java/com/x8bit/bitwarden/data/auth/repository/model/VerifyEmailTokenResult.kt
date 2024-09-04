package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Models result of verifying the email token.
 */
sealed class VerifyEmailTokenResult {

    /**
     * Represents a successful verification of email token.
     */
    data object Verified : VerifyEmailTokenResult()

    /**
     * Represents an expired email verification token.
     */
    data object LinkExpired : VerifyEmailTokenResult()

    /**
     * There was an error verifying email token.
     */
    data object Error : VerifyEmailTokenResult()
}
