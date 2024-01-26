package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Models result of resend email request.
 */
sealed class ResendEmailResult {

    /**
     * Resend email request success
     */
    data object Success : ResendEmailResult()

    /**
     * There was an error.
     */
    data class Error(val message: String?) : ResendEmailResult()
}
