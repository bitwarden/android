package com.x8bit.bitwarden.data.autofill.password.model

/**
 * Represents the result of a FIDO 2 Get Credentials request.
 */
sealed class PasswordGetCredentialsResult {
    /**
     * Indicates credentials were successfully queried.
     */
    data class Success(
        val data: String
    ) : PasswordGetCredentialsResult()

    /**
     * Indicates an error was encountered when querying for matching credentials.
     */
    data object Error : PasswordGetCredentialsResult()
}
