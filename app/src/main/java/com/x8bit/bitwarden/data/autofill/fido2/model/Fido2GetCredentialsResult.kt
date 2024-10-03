package com.x8bit.bitwarden.data.autofill.fido2.model

import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import com.bitwarden.fido.Fido2CredentialAutofillView

/**
 * Represents the result of a FIDO 2 Get Credentials request.
 */
sealed class Fido2GetCredentialsResult {
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
    ) : Fido2GetCredentialsResult()

    /**
     * Indicates an error was encountered when querying for matching credentials.
     */
    data object Error : Fido2GetCredentialsResult()
}
