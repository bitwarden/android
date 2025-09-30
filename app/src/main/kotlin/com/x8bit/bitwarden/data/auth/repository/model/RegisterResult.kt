package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Models result of registering a new account.
 */
sealed class RegisterResult {
    /**
     * Register succeeded.
     *
     */
    data object Success : RegisterResult()

    /**
     * There was an error logging in.
     *
     * @param errorMessage a message describing the error.
     */
    data class Error(
        val errorMessage: String?,
        val error: Throwable?,
    ) : RegisterResult()

    /**
     * Password hash was found in a data breach.
     */
    data object DataBreachFound : RegisterResult()

    /**
     * Password hash was found to be weak.
     */
    data object WeakPassword : RegisterResult()

    /**
     * Password hash was found in a data breach and found to be weak.
     */
    data object DataBreachAndWeakPassword : RegisterResult()
}
