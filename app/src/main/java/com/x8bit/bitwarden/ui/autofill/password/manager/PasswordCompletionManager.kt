package com.x8bit.bitwarden.ui.autofill.password.manager

import com.x8bit.bitwarden.data.autofill.password.model.PasswordCredentialAssertionResult
import com.x8bit.bitwarden.data.autofill.password.model.PasswordGetCredentialsResult
import com.x8bit.bitwarden.data.autofill.password.model.PasswordRegisterCredentialResult

/**
 * A manager for completing the Password creation process.
 */
interface PasswordCompletionManager {

    /**
     * Completes the Password registration process with the provided [result].
     */
    fun completePasswordRegistration(result: PasswordRegisterCredentialResult)

    /**
     * Completes the Password registration process with the provided [result].
     */
    fun completePasswordAssertion(result: PasswordCredentialAssertionResult)

    /**
     * Complete the Password "Get credentials" process with the provided [result].
     */
    fun completePasswordGetCredentialRequest(result: PasswordGetCredentialsResult)
}
