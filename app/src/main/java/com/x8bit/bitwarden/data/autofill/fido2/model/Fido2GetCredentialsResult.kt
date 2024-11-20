package com.x8bit.bitwarden.data.autofill.fido2.model

import androidx.credentials.provider.CredentialEntry

/**
 * Represents the result of a FIDO 2 Get Credentials request.
 */
sealed class Fido2GetCredentialsResult {
    /**
     * Indicates credentials were successfully queried.
     *
     * @param credentials Collection of [CredentialEntry]s matching the original request
     * parameters. This may be an empty list if no matching values were found.
     */
    data class Success(
        val credentials: List<CredentialEntry>,
    ) : Fido2GetCredentialsResult()

    /**
     * Indicates an error was encountered when querying for matching credentials.
     */
    data object Error : Fido2GetCredentialsResult()
}
