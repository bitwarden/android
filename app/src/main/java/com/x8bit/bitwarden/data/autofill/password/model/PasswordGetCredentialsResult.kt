package com.x8bit.bitwarden.data.autofill.password.model

import androidx.credentials.provider.CredentialEntry

/**
 * Represents the result of a Password Get Credentials request.
 */
sealed class PasswordGetCredentialsResult {
    /**
     * Indicates credentials were successfully queried.
     */
    data class Success(
        val credentials: List<CredentialEntry>,
    ) : PasswordGetCredentialsResult()

    /**
     * Indicates an error was encountered when querying for matching credentials.
     */
    data object Error : PasswordGetCredentialsResult()
}
