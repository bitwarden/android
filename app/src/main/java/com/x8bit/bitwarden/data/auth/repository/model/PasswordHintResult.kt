package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Models result of password hint request.
 */
sealed class PasswordHintResult {

    /**
     * Password hint request success
     */
    data object Success : PasswordHintResult()

    /**
     * There was an error.
     */
    data class Error(val message: String?) : PasswordHintResult()
}
