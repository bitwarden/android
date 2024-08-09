package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Models result of sending a verification email.
 */
sealed class SendVerificationEmailResult {
    /**
     * Email sent succeeded.
     *
     * @param emailVerificationToken the token to verify the email.
     */
    data class Success(
        val emailVerificationToken: String?,
    ) : SendVerificationEmailResult()

    /**
     * There was an error sending the email.
     *
     * @param errorMessage a message describing the error.
     */
    data class Error(val errorMessage: String?) : SendVerificationEmailResult()
}
