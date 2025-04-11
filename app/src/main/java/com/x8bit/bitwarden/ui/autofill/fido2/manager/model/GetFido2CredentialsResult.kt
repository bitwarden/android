package com.x8bit.bitwarden.ui.autofill.fido2.manager.model

import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import com.bitwarden.fido.Fido2CredentialAutofillView
import com.bitwarden.ui.util.Text

/**
 * Represents the result of fetching FIDO2 credentials.
 */
sealed class GetFido2CredentialsResult {

    /**
     * Represents a successful result with a list of matching credentials, the original request
     * option, and associated user ID.
     */
    data class Success(
        val credentials: List<Fido2CredentialAutofillView>,
        val option: BeginGetPublicKeyCredentialOption,
        val userId: String,
    ) : GetFido2CredentialsResult()

    /**
     * Represents an error result with a message.
     *
     * @property message The error message.
     */
    data class Error(val message: Text) : GetFido2CredentialsResult()
}
