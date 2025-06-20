package com.x8bit.bitwarden.ui.credentials.manager.model

import com.bitwarden.ui.util.Text

/**
 * Represents the result of FIDO2 authentication.
 */
sealed class AssertFido2CredentialResult {
    /**
     * Represents a successful authentication of FIDO2 credentials.
     */
    data class Success(val responseJson: String) : AssertFido2CredentialResult()

    /**
     * Indicates the user cancelled authentication.
     */
    data object Cancelled : AssertFido2CredentialResult()

    /**
     * Represents an error during FIDO2 credential assertion.
     */
    data class Error(val message: Text) : AssertFido2CredentialResult()
}
