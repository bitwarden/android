package com.x8bit.bitwarden.ui.credentials.manager.model

import androidx.credentials.provider.CredentialEntry
import com.bitwarden.ui.util.Text

/**
 * Represents the result of fetching credentials for the Credential Provider Service.
 */
sealed class GetCredentialsResult {

    /**
     * Represents a successful result with a list of matching credentials, the original request
     * option, and associated user ID.
     */
    data class Success(
        val credentialEntries: List<CredentialEntry>,
        val userId: String,
    ) : GetCredentialsResult()

    /**
     * Represents an error result with a message.
     *
     * @property message The error message.
     */
    data class Error(val message: Text) : GetCredentialsResult()
}
