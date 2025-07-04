package com.x8bit.bitwarden.ui.credentials.manager.model

import com.bitwarden.ui.util.Text
import com.bitwarden.vault.LoginView

/**
 * Represents the result of Password authentication.
 */
sealed class GetPasswordCredentialResult {
    /**
     * Represents a successful authentication of Password credentials.
     */
    data class Success(val credential: LoginView) : GetPasswordCredentialResult()

    /**
     * Indicates the user cancelled authentication.
     */
    data object Cancelled : GetPasswordCredentialResult()

    /**
     * Represents an error during Password credential assertion.
     */
    data class Error(val message: Text) : GetPasswordCredentialResult()
}
