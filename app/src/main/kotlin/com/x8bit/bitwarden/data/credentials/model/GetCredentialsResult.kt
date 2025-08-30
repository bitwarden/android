package com.x8bit.bitwarden.data.credentials.model

import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import com.bitwarden.fido.Fido2CredentialAutofillView
import com.bitwarden.ui.util.Text

/**
 * Represents the result of a FIDO 2 Get Credentials request.
 */
sealed class GetCredentialsResult {
    /**
     * Indicates credentials were successfully queried.
     *
     * @param userId ID of the user whose credentials were queried.
     * @param options Original request options provided by the relying party.
     * @param credentials Collection of [Fido2CredentialAutofillView]s matching the original request
     * parameters. This may be an empty list if no matching values were found.
     */
    data class Success(
        val userId: String,
        val options: BeginGetPublicKeyCredentialOption,
        val credentials: List<Fido2CredentialAutofillView>,
    ) : GetCredentialsResult()

    /**
     * Indicates an error was encountered when querying for matching credentials.
     */
    data class Error(
        val message: Text,
    ) : GetCredentialsResult()
}
