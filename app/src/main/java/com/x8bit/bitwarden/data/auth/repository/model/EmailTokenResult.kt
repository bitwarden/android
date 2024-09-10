package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Model the result of a request to validate a given email token.
 */
sealed class EmailTokenResult {

    /**
     * The token is valid and the user can proceed with account creation.
     */
    data object Success : EmailTokenResult()

    /**
     * The token has expired and is no longer valid.
     */
    data object Expired : EmailTokenResult()

    /**
     * There was an error validating the token.
     */
    data class Error(val message: String?) : EmailTokenResult()
}
