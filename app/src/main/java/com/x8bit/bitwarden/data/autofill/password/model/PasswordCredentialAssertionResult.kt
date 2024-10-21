package com.x8bit.bitwarden.data.autofill.password.model

import com.bitwarden.vault.LoginView

/**
 * Represents possible outcomes of a Password credential assertion request.
 */
sealed class PasswordCredentialAssertionResult {

    /**
     * Indicates the assertion request completed and [credential] was successfully generated.
     */
    data class Success(val credential: LoginView) : PasswordCredentialAssertionResult()

    /**
     * Indicates there was an error and the assertion was not successful.
     */
    data object Error : PasswordCredentialAssertionResult()
}
